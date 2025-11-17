package app.paralino.core.repo.places

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.presentation.places.detail.PlaceSensitivityDelayType
import app.paralino.core.presentation.places.detail.PlaceSensitivityLocationRefreshType
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
    abstract suspend fun processLocation(data: LocationDataModel)
}

class IOSLSEPlacesActivityProcessor : IPlacesActivityProcessor() {
    override suspend fun processLocation(data: LocationDataModel) {}
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
    private val appPrefs: AppPrefs
) : IPlacesActivityProcessor() {

    override suspend fun processLocation(data: LocationDataModel) {
        // TODO filter out frequent calls and calculations of distance between points
        logg.v("place_activity", "Process new location in places")

        placeActivityUploadEngine.syncPendingEventsInBackground()

        val places = local.getAllPlacesOnce().filter {
            locationSharingManager.isSharingActiveForGroup(it.groupOrSpaceId) && it.canSendAlert
        }
        places.forEach { place ->
            place.data.data?.let { placeData ->
                val movementType = getMovementType(
                    location = data,
                    place = place,
                    placeData = placeData
                )
                val previousState = placeActivityRepo.getPlaceActivity(placeId = place.id)

//                logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

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
        }
    }

    fun placesEnter(placesIds: List<String>, trigger: PlaceActivityTriggerType) {
        logg.i("place_activity_geofence", "User entered geofence")
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val groupId = local.getGroupIdFromPlaceId(placeId = placeId)!!
                    startPlaceNotifySchedule(
                        placeId = placeId,
                        groupId = groupId,
                        enter = true,
                        trigger = trigger
                    )
                } catch (e: Exception) {
                    logg.w("place_activity", "Error notifying place enter: ${e.message}")
                }
            }
        }
    }

    fun placesExit(placesIds: List<String>, trigger: PlaceActivityTriggerType) {
        logg.i("place_activity_geofence", "User exited geofence")
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val groupId = local.getGroupIdFromPlaceId(placeId = placeId)!!
                    startPlaceNotifySchedule(
                        placeId = placeId,
                        groupId = groupId,
                        enter = false,
                        trigger = trigger
                    )
                } catch (e: Exception) {
                    logg.w("place_activity", "Error notifying place exit: ${e.message}")
                }
            }
        }
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
                    location = location,
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
                location.accuracy.isLocationInaccurate() -> true
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
                    durationMillis = duration.inWholeMilliseconds
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
            it.accuracy < 30 && !it.isOlderThan(10.seconds)
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
        location: LocationDataModel,
        place: GroupPlace,
        placeData: GroupPlaceData
    ): PlaceActivityMovementType {
        val distance = geofenceManager.distanceBetweenPoints(
            data = location,
            geofence = GroupPlaceGeofenceData(
                groupId = place.groupOrSpaceId,
                placeId = place.id,
                lat = placeData.lat,
                lng = placeData.lng,
                radiusMeters = placeData.radiusMeters
            )
        )

        // user entered place only if location (lat, lng) fully entered place circle
        // if user accuracy is not great, accept place enter only if user center is closer to place center
        val accuracyThreshold = if (location.accuracy > 50) location.accuracy * 0.1f else 0f
        val radiusMatch = when {
            // full user location fits inside place
            placeData.radiusMeters > location.accuracy -> true
            // > 80% of area can overlap (place circle is at least 80% of user radius)
            placeData.radiusMeters / location.accuracy > 0.8 -> {
                // allow not so accurate location only if it has decent overlap
                distance < location.accuracy - placeData.radiusMeters
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
            // TODO check for coarse location
            location.accuracy > 500 -> {
                // if accuracy is quite bad, only consider exit if user is very far away
                2000f
            }

            location.accuracy >= 100 -> {
                // if accuracy is not that great, only consider exit if user is  somewhat far away
                if (place.alertSensitivity.ignoreInaccurate) {
                    location.accuracy * 2
                } else {
                    when (DeviceType.current) {
                        DeviceType.Android -> {
                            when (location.accuracy) {
                                // "magic numbers" because android devices sometimes produce completely wrong locations
                                // with round number accuracy in low hundreds, while actual accuracy should be at least double that
                                // In case that reported location is actually within that circle
                                // this won't cause an issue since it will just cause device to request better location
                                100f, 200f, 300f, 400f, 500f -> location.accuracy * 2f
                                else -> location.accuracy / 2
                            }
                        }

                        DeviceType.iOS -> {
                            location.accuracy / 2
                        }
                    }
                }
            }

            else -> {
                // if accuracy is decent,
                if (place.alertSensitivity.ignoreInaccurate) {
                    (location.accuracy / 10).coerceAtLeast(5f)
                } else {
                    0f
                }
            }
        }
        val isExit = distance > placeData.radiusMeters + location.accuracy + exitThreshold

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