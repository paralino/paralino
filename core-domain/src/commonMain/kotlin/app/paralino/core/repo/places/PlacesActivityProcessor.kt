package app.paralino.core.repo.places

import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.location.PrivacyZoneObfuscator
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.models.places.PlaceSensitivityDelayType
import app.paralino.core.models.places.PlaceSensitivityLocationRefreshType
import app.paralino.core.provider.ConnectedWifi
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.provider.current
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.extensions.isLocationInaccurate
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

abstract class IPlacesActivityProcessor {
    abstract suspend fun processLocation(mappedLocations: List<MemberGroupId>)
}

class IOSLSEPlacesActivityProcessor : IPlacesActivityProcessor() {
    override suspend fun processLocation(mappedLocations: List<MemberGroupId>) {}
}

class PlacesActivityProcessor(
    private val local: PlacesLocalDataSource,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val placeActivityUploadEngine: PlaceActivityUploadEngine,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val geofenceManager: IGeofenceManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val locationSharingManager: LocationSharingManager,
    private val oneTimeLocation: IOneTimeLocationProvider,
    private val refreshEverythingUseCase: RefreshEverythingUseCase,
    private val privacyZoneObfuscator: PrivacyZoneObfuscator,
    private val falseAlertMemory: FalsePlaceAlertMemory,
    private val wifiAnchorMemory: WifiAnchorMemory,
    private val connectionStatus: IConnectionStatusProvider,
) : IPlacesActivityProcessor() {

    override suspend fun processLocation(mappedLocations: List<MemberGroupId>) {
        logg.v("place_activity", "Process new locations ${mappedLocations.size} in places")

        placeActivityUploadEngine.syncPendingEventsInBackground()

        if (mappedLocations.isEmpty()) {
            return
        }

        val wifi = connectionStatus.getConnectedWifi()
        mappedLocations
            .mapNotNull { it.locationData }
            .firstOrNull { !it.isApproximate && !it.isInPrivacyZone && !it.isMasked }
            ?.let { wifiAnchorMemory.processLocation(wifi = wifi, location = it) }

        local.getAllPlacesForGroupsOnce(groupIds = mappedLocations.map { it.groupId })
            .filter { shouldProcessPlaceForLocation(it) }
            .forEach { place ->
                val locationForGroup = mappedLocations
                    .firstOrNull { it.groupId == place.groupOrSpaceId }
                    ?.locationData

                if (locationForGroup != null) {
                    processLocationForPlace(
                        place = place,
                        mappedLocation = locationForGroup,
                        wifi = wifi
                    )
                }
            }
    }

    private suspend fun shouldProcessPlaceForLocation(place: GroupPlace): Boolean {

        if (!place.canSendAlert) {
            return false
        }

        val contract = locationSharingManager.currentSharingContractForGroup(place.groupOrSpaceId)
            ?: return false

        val approximateLocationRadiusCheck = contract.approximateLocationInMeters?.let {
            (place.data.data?.radiusMeters ?: 0) > it
        } ?: true

        if (!approximateLocationRadiusCheck) {
            return false
        }

        return !isPlaceInPrivacyZone(place)
    }

    private suspend fun isPlaceInPrivacyZone(place: GroupPlace): Boolean {
        val placeData = place.data.data ?: return false
        return privacyZoneObfuscator.isPlaceOverlappingPrivacyZone(
            groupId = place.groupOrSpaceId,
            lat = placeData.lat,
            lng = placeData.lng,
            radiusMeters = placeData.radiusMeters,
        )
    }

    private suspend fun processLocationForPlace(
        place: GroupPlace,
        mappedLocation: LocationDataModel,
        wifi: ConnectedWifi?
    ) {
        val placeData = place.data.data ?: return

        val movementType = getMovementType(
            locationLat = mappedLocation.lat,
            locationLng = mappedLocation.lng,
            locationAccuracy = mappedLocation.accuracy,
            place = place,
            placeData = placeData
        )
        val previousState = placeActivityRepo.getPlaceActivity(placeId = place.id)

//      logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

        if (movementType == PlaceActivityMovementType.ENTER) {
            wifiAnchorMemory.resetExit(placeId = place.id)
        } else if (movementType == PlaceActivityMovementType.EXIT && previousState?.isEnter == true &&
            wifiAnchorMemory.shouldIgnoreExit(
                placeId = place.id,
                wifi = wifi,
                placeCenter = LatLng(lat = placeData.lat, lng = placeData.lng),
                radiusMeters = placeData.radiusMeters.toFloat()
            )
        ) {
            return
        }

        // location from a known inaccurate spot must not change place state (no alert, no location boost)
        val verdict = falseAlertMemory.processLocation(
            placeId = place.id,
            previousIsEnter = previousState?.isEnter,
            movementIsEnter = when (movementType) {
                PlaceActivityMovementType.ENTER -> true
                PlaceActivityMovementType.EXIT -> false
                PlaceActivityMovementType.UNKNOWN -> null
            },
            location = LatLng(lat = mappedLocation.lat, lng = mappedLocation.lng)
        )
        if (verdict == FalsePlaceAlertMemory.FixVerdict.IGNORE) {
            return
        }

        when (movementType) {
            PlaceActivityMovementType.ENTER -> {
                startPlaceNotifySchedule(
                    placeId = place.id,
                    groupId = place.groupOrSpaceId,
                    enter = true,
                    trigger = PlaceActivityTriggerType.LOCATION
                )
            }

            PlaceActivityMovementType.EXIT -> {
                startPlaceNotifySchedule(
                    placeId = place.id,
                    groupId = place.groupOrSpaceId,
                    enter = false,
                    trigger = PlaceActivityTriggerType.LOCATION
                )
            }

            PlaceActivityMovementType.UNKNOWN -> {
                // else -> there is no certain change to place activity, or it is the same as previous one
                if (previousState?.isEnter == true) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupOrSpaceId,
                        enter = true,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                } else if (previousState?.isEnter == false) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupOrSpaceId,
                        enter = false,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                }
            }
        }
    }

    fun placesEnter(placesIds: List<String>, trigger: PlaceActivityTriggerType) {
        logg.i("place_activity_geofence", "User entered geofence")
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val place = local.getPlaceWithoutSettingsOnce(placeId = placeId) ?: return@forEach
                    if (isPlaceInPrivacyZone(place)) {
                        logg.i("place_activity_geofence", "Skip place enter from geofence - place in privacy zone")
                        return@forEach
                    }

                    val groupId = place.groupOrSpaceId
                    val contract = locationSharingManager.currentSharingContractForGroup(id = groupId)
                    if (contract?.approximateLocationInMeters != null) {
                        logg.i("place_activity_geofence", "Skip place enter from geofence - approximate location on")
                        // this will trigger regular location flow so we get obfuscated location for processing
                        locationPerformanceManager.locationBoostRequested(
                            request = LocationPerformanceRequest(
                                type = LocationPerformanceType.HIGH,
                                duration = 10.seconds
                            ),
                            groupId = groupId,
                            from = "place enter geofence"
                        )
                    } else {
                        startPlaceNotifySchedule(
                            placeId = placeId,
                            groupId = groupId,
                            enter = true,
                            trigger = trigger
                        )
                    }
                } catch (e: Exception) {
                    logg.w("place_activity", "Error notifying place enter: ${e.message}")
                }
            }

            refreshEverythingUseCase.refreshEverythingFallback()
        }
    }

    fun placesExit(placesIds: List<String>, trigger: PlaceActivityTriggerType) {
        logg.i("place_activity_geofence", "User exited geofence")
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val place = local.getPlaceWithoutSettingsOnce(placeId = placeId) ?: return@forEach
                    if (isPlaceInPrivacyZone(place)) {
                        logg.i("place_activity_geofence", "Skip place exit from geofence - place in privacy zone")
                        return@forEach
                    }

                    val groupId = place.groupOrSpaceId
                    val contract = locationSharingManager.currentSharingContractForGroup(id = groupId)
                    if (contract?.approximateLocationInMeters != null) {
                        logg.i("place_activity_geofence", "Skip place exit from geofence - approximate location on")
                        // this will trigger regular location flow so we get obfuscated location for processing
                        locationPerformanceManager.locationBoostRequested(
                            request = LocationPerformanceRequest(
                                type = LocationPerformanceType.HIGH,
                                duration = 10.seconds
                            ),
                            groupId = groupId,
                            from = "place exit geofence"
                        )
                    } else if (isOnPlaceWifi(place)) {
                        logg.i("place_activity_geofence", "Ignore geofence exit - on place wifi - $placeId")
                    } else {
                        startPlaceNotifySchedule(
                            placeId = placeId,
                            groupId = groupId,
                            enter = false,
                            trigger = trigger
                        )
                    }
                } catch (e: Exception) {
                    logg.w("place_activity", "Error notifying place exit: ${e.message}")
                }
            }

            refreshEverythingUseCase.refreshEverythingFallback()
        }
    }

    private suspend fun isOnPlaceWifi(place: GroupPlace): Boolean {
        val placeData = place.data.data ?: return false
        return wifiAnchorMemory.isOnPlaceWifi(
            wifi = connectionStatus.getConnectedWifi(),
            placeCenter = LatLng(lat = placeData.lat, lng = placeData.lng),
            radiusMeters = placeData.radiusMeters.toFloat()
        )
    }

    private suspend fun startPlaceNotifySchedule(
        placeId: String,
        groupId: GroupOrSpaceId,
        enter: Boolean,
        trigger: PlaceActivityTriggerType
    ) {

        val sourceTag = trigger.key
        val activityTag = if (enter) "${sourceTag}_enter" else "${sourceTag}_exit"

        val oldState = placeActivityRepo.getPlaceActivity(placeId)
        val newState = if (oldState != null) {
            oldState.copy(
                isEnter = enter,
                stateChangeTrigger = trigger,
                stateChangeTimeMillis = if (oldState.isEnter == enter) {
                    oldState.stateChangeTimeMillis
                } else {
                    nowUTC().toEpochMilliseconds()
                }
            )
        } else {
            PlaceActivityState(
                placeId = placeId,
                groupId = groupId,
                isEnter = enter,
                stateChangeTrigger = trigger,
                stateChangeTimeMillis = nowUTC().toEpochMilliseconds()
            )
        }

        placeActivityRepo.updatePlaceActivity(
            placeId = placeId,
            state = newState
        )

        if (newState.isEnter == oldState?.isScheduledStateEnter || newState.isEnter == oldState?.isEnter) {
//            logg.tag("place_activity")
//                .d("Cancel place $activityTag notify - state did not change - $placeId")
            if (oldState.tryNotifyJob == null && oldState.isScheduledStateEnter != null) {
//                logg.tag("place_activity").d("Could reschedule $activityTag notify - $placeId")

                // replace state change time with old value so we can send time difference to server
                // this will then show more accurate place change time in notification
                val newTime = if (Instant.fromEpochMilliseconds(oldState.stateChangeTimeMillis)
                        .isOlderThan(12.hours)
                ) {
                    // ignore old time if it is too old
                    newState.stateChangeTimeMillis
                } else {
                    oldState.stateChangeTimeMillis
                }
                schedulePlaceNotify(
                    activityTag = activityTag,
                    placeId = placeId,
                    enter = enter,
                    oldState = oldState,
                    newState = newState.copy(stateChangeTimeMillis = newTime),
                    trigger = trigger
                )
            }
            return
        }

        if (oldState?.tryNotifyJob?.isActive == true) {
//            logg.tag("place_activity").d("Place notify already scheduled $activityTag - $placeId")
            return
        }

        if (oldState == null || (oldState.isEnter == enter && oldState.isLastNotifyStateEnter != !enter)) {
//            logg.tag("place_activity")
//                .d("Cancel place $activityTag notify (state: ${oldState?.isEnter}) - $placeId")
            return
        }

        if (oldState.isLastNotifyStateEnter == enter && nowUTC().toEpochMilliseconds()
                .minus(oldState.notifyTimeMillis ?: 0) < 1.hours.inWholeMilliseconds
        ) {
//            logg.tag("place_activity")
//                .d("Cancel place $activityTag notify (already notified) - $placeId")
            return
        }

        schedulePlaceNotify(
            activityTag = activityTag,
            placeId = placeId,
            enter = enter,
            oldState = oldState,
            newState = newState,
            trigger = trigger
        )
    }

    private suspend fun schedulePlaceNotify(
        activityTag: String,
        placeId: String,
        enter: Boolean,
        oldState: PlaceActivityState,
        newState: PlaceActivityState,
        trigger: PlaceActivityTriggerType
    ) {
        logg.tag("place_activity").i("Schedule place notify $activityTag - $placeId")
        oldState.tryNotifyJob?.cancel()

        val place = local.getPlaceWithoutSettingsOnce(placeId)
        if (place == null) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (place null) - $placeId")
            placeActivityRepo.updatePlaceActivity(
                placeId = placeId,
                state = null
            )
            return
        }

        val updateJob = scopeProvider.userSessionIOScope.launch {
            val delay = placeActivityDelay(
                place = place,
                trigger = trigger
            )

            logg.tag("place_activity")
                .i("Delay place notify $activityTag - $placeId for ${delay?.inWholeSeconds}")

            if (delay != null) {
                if (place.alertSensitivity.sensitivityRefresh == PlaceSensitivityLocationRefreshType.BEFORE) {
                    updateDeviceStatusAndLocation(
                        activityTag = activityTag,
                        trigger = trigger,
                        place = place
                    )
                }

                delay(delay)
            }

            try {
                processPlaceActivity(
                    activityTag = activityTag,
                    place = place
                )
            } catch (e: Exception) {
                logg.w("place_activity", "Error notifying place exit: ${e.message}")
            }
        }

        placeActivityRepo.updatePlaceActivity(
            placeId = placeId,
            state = newState.copy(
                isScheduledStateEnter = enter,
                scheduleTrigger = trigger,
                tryNotifyJob = updateJob
            )
        )
    }

    private suspend fun processPlaceActivity(
        activityTag: String,
        place: GroupPlace
    ) {
        val placeId = place.id
        val currentState = placeActivityRepo.getPlaceActivity(placeId = placeId)

        if (currentState == null) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify - state is null - $placeId")
            return
        }

        if (currentState.isEnter != currentState.isScheduledStateEnter) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify - state does not match - $placeId (state: ${currentState.isEnter}, scheduled: ${currentState.isScheduledStateEnter})")
            placeActivityRepo.updatePlaceActivity(
                placeId = placeId,
                state = currentState.copy(
                    isScheduledStateEnter = null,
                    scheduleTrigger = null
                )
            )
            return
        }

        val notifyTimeDiff = (currentState.notifyTimeMillis ?: 0)
            .minus(nowUTC().toEpochMilliseconds()).absoluteValue

        // already notified this place in last period - this will filter frequent updates
        // but if user stays more than certain period inside the space, it will trigger notification
        if (notifyTimeDiff < 10.seconds.inWholeMilliseconds) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (notify diff) - $placeId")
            placeActivityRepo.updatePlaceActivity(
                placeId = placeId,
                state = currentState.copy(
                    isScheduledStateEnter = null,
                    scheduleTrigger = null
                )
            )
            return
        }

        if (!locationSharingManager.isSharingActiveForGroup(place.groupOrSpaceId)) {
            // reset place activity if sharing stops
            placeActivityRepo.updatePlaceActivity(placeId = placeId, state = null)
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (sharing off) - $placeId")
            return
        }

        if (!place.canSendAlert) {
            // reset place activity if place is not capable of alerts
            placeActivityRepo.updatePlaceActivity(placeId = placeId, state = null)
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (place without alerts) - $placeId")
            return
        }

        if (place.alertSensitivity.ignoreInaccurate) {
            val location = oneTimeLocation.getLastLocation(
                requestType = IOneTimeLocationProvider.RequestType.CacheOnly
            )
            val placeData = place.data.data
            val isLocationInaccurate = if (location != null && placeData != null) {

                val movementType = getMovementType(
                    locationLat = location.lat,
                    locationLng = location.lng,
                    locationAccuracy = location.adjustedHorizontalAccuracy,
                    place = place,
                    placeData = placeData
                )

                when (movementType) {
                    PlaceActivityMovementType.ENTER -> !currentState.isEnter
                    PlaceActivityMovementType.EXIT -> currentState.isEnter
                    PlaceActivityMovementType.UNKNOWN -> true
                }
            } else {
                true
            }

            if (isLocationInaccurate) {
                placeActivityRepo.updatePlaceActivity(placeId = placeId, state = null)
                logg.tag("place_activity")
                    .d("Cancel place $activityTag notify (location not accurate) - $placeId")
                return
            }
        }

        logg.tag("place_activity").i("Trigger notify place $activityTag - $placeId")
        placeActivityRepo.updatePlaceActivity(
            placeId = placeId,
            state = currentState.copy(
                isLastNotifyStateEnter = currentState.isEnter,
                notifyTimeMillis = nowUTC().toEpochMilliseconds(),
                notifyTrigger = currentState.stateChangeTrigger,
                isScheduledStateEnter = null,
                scheduleTrigger = null
            )
        )

        try {
            placeActivityUploadEngine.notifyPlaceEvent(
                place = place,
                isEnter = currentState.isEnter,
                timeMillis = currentState.stateChangeTimeMillis
            )

            // update device status with last location if place activity came from geofence
            if (place.alertSensitivity.sensitivityRefresh == PlaceSensitivityLocationRefreshType.AFTER) {
                updateDeviceStatusAndLocation(
                    activityTag = activityTag,
                    trigger = currentState.scheduleTrigger,
                    place = place
                )
            }
        } catch (e: Exception) {
            placeActivityRepo.updatePlaceActivity(placeId = placeId, state = null)
            throw e
        }
    }

    private suspend fun updateDeviceStatusAndLocation(
        activityTag: String,
        trigger: PlaceActivityTriggerType?,
        place: GroupPlace
    ) {
        if (trigger == PlaceActivityTriggerType.GOEFENCE) {
            scopeProvider.userSessionIOScope.launch {
                // update device status with last location if place activity came from geofence
                try {
                    deviceStatusProcessor.pushDeviceStatus(groupId = place.groupOrSpaceId)
                } catch (e: Exception) {
                    logg.tag("place_activity")
                        .w("Error updating member status in ${place.groupOrSpaceId} from $activityTag - ${place.id}: ${e.message}")
                }
            }
        }

        val shouldRefresh = if (place.alertSensitivity.refreshOnlyInaccurate) {
            val location = oneTimeLocation.getLastLocation(
                requestType = IOneTimeLocationProvider.RequestType.CacheOnly
            )

            when {
                location == null -> true
                location.adjustedHorizontalAccuracy.isLocationInaccurate() -> true
                location.timeUTCInstant?.isOlderThan(5.minutes) != false -> true
                else -> false
            }
        } else {
            true
        }

        if (shouldRefresh) {
            val duration = when (place.alertSensitivity.sensitivityDelay) {
                PlaceSensitivityDelayType.NONE -> 12.seconds
                PlaceSensitivityDelayType.MINIMUM -> 6.seconds
                PlaceSensitivityDelayType.BALANCED -> 12.seconds
                PlaceSensitivityDelayType.LONG -> 20.seconds
            }

            locationPerformanceManager.locationBoostRequested(
                request = LocationPerformanceRequest(
                    type = LocationPerformanceType.HIGH,
                    duration = duration
                ),
                groupId = place.groupOrSpaceId,
                from = "place $activityTag"
            )
        }
    }

    private suspend fun placeActivityDelay(
        place: GroupPlace?,
        trigger: PlaceActivityTriggerType
    ): Duration? {
        if (place == null) {
            return null
        }

        val location = oneTimeLocation.getLastLocation(
            requestType = IOneTimeLocationProvider.RequestType.CacheOnly
        )
        val isLocationVeryAccurate = location?.let {
            it.adjustedHorizontalAccuracy < 30 && !it.isOlderThan(10.seconds)
        } ?: false

        return when (trigger) {
            PlaceActivityTriggerType.GOEFENCE -> {
                when (place.alertSensitivity.sensitivityDelay) {
                    PlaceSensitivityDelayType.NONE -> {
                        null
                    }

                    PlaceSensitivityDelayType.MINIMUM -> {
                        if (isLocationVeryAccurate) null
                        else 6.seconds
                    }

                    PlaceSensitivityDelayType.BALANCED -> {
                        if (isLocationVeryAccurate) 4.seconds
                        else 12.seconds
                    }

                    PlaceSensitivityDelayType.LONG -> {
                        20.seconds
                    }
                }
            }

            else -> {
                when (place.alertSensitivity.sensitivityDelay) {
                    PlaceSensitivityDelayType.NONE -> {
                        if (isLocationVeryAccurate) null
                        else 5.seconds
                    }

                    PlaceSensitivityDelayType.MINIMUM -> {
                        if (isLocationVeryAccurate) 5.seconds
                        else 10.seconds
                    }

                    PlaceSensitivityDelayType.BALANCED -> {
                        if (isLocationVeryAccurate) 10.seconds
                        else 15.seconds
                    }

                    PlaceSensitivityDelayType.LONG -> {
                        if (isLocationVeryAccurate) 15.seconds
                        else 25.seconds
                    }
                }
            }
        }
    }

    private fun getMovementType(
        locationLat: Double,
        locationLng: Double,
        locationAccuracy: Float,
        place: GroupPlace,
        placeData: GroupPlaceData
    ): PlaceActivityMovementType {
        val distance = geofenceManager.distanceBetweenPoints(
            lat1 = locationLat,
            lng1 = locationLng,
            lat2 = placeData.lat,
            lng2 = placeData.lng
        )

        // user entered place only if location (lat, lng) fully entered place circle
        // if user accuracy is not great, accept place enter only if user center is closer to place center
        val accuracyThreshold = if (locationAccuracy > 50) locationAccuracy * 0.1f else 0f
        val radiusMatch = when {
            // full user location fits inside place
            placeData.radiusMeters > locationAccuracy -> true
            // > 80% of area can overlap (place circle is at least 80% of user radius)
            placeData.radiusMeters / locationAccuracy > 0.8 -> {
                // allow not so accurate location only if it has decent overlap
                distance < locationAccuracy - placeData.radiusMeters
            }

            else -> false
        }
        val isEnter = (distance + accuracyThreshold) < placeData.radiusMeters && radiusMatch

        if (isEnter) {
            return PlaceActivityMovementType.ENTER
        }

        // user exited place if user's accuracy circle is not overlapping place circle
        // this is used to help filter location jumping when user stays in one closed place (like house)
        // in that case accuracy tends to worsen so in order to check for place exit we want to make sure user definitely left place
        // also adding few meters of buffer to improve filter
        val exitThreshold = when {
            locationAccuracy > 500 -> {
                // if accuracy is quite bad, only consider exit if user is very far away
                2000f
            }

            locationAccuracy >= 100 -> {
                // if accuracy is not that great, only consider exit if user is  somewhat far away
                if (place.alertSensitivity.ignoreInaccurate) {
                    locationAccuracy * 2
                } else {
                    when (DeviceType.current) {
                        DeviceType.Android -> {
                            when (locationAccuracy) {
                                // "magic numbers" because android devices sometimes produce completely wrong locations
                                // with round number accuracy in low hundreds, while actual accuracy should be at least double that
                                // In case that reported location is actually within that circle
                                // this won't cause an issue since it will just cause device to request better location
                                100f, 200f, 300f, 400f, 500f -> locationAccuracy * 2f
                                else -> locationAccuracy / 2
                            }
                        }

                        DeviceType.iOS -> {
                            locationAccuracy / 2
                        }
                    }
                }
            }

            else -> {
                // if accuracy is decent,
                if (place.alertSensitivity.ignoreInaccurate) {
                    (locationAccuracy / 10).coerceAtLeast(5f)
                } else {
                    0f
                }
            }
        }
        val isExit = distance > placeData.radiusMeters + locationAccuracy + exitThreshold

        if (isExit) {
            return PlaceActivityMovementType.EXIT
        }

        return PlaceActivityMovementType.UNKNOWN
    }
}

enum class PlaceActivityTriggerType constructor(val key: String) {
    GOEFENCE(key = "geofence"),
    LOCATION(key = "location"),
    GENERIC(key = "generic");

    companion object {
        fun fromKey(key: String): PlaceActivityTriggerType {
            return entries.firstOrNull { it.key == key } ?: GENERIC
        }
    }
}

private enum class PlaceActivityMovementType {
    ENTER, EXIT, UNKNOWN
}