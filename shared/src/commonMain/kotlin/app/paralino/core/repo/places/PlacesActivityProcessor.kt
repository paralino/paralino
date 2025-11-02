package app.paralino.core.repo.places

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.presentation.places.detail.PlaceSensitivityDelayType
import app.paralino.core.presentation.places.detail.PlaceSensitivityLocationRefreshType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
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
                val distance = geofenceManager.distanceBetweenPoints(
                    data = data,
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
                val accuracyThreshold = if (data.accuracy > 50) data.accuracy * 0.1f else 0f
                val radiusMatch = when {
                    // full user location fits inside place
                    placeData.radiusMeters > data.accuracy -> true
                    // > 80% of area can overlap (place circle is at least 80% of user radius)
                    placeData.radiusMeters / data.accuracy > 0.8 -> {
                        // allow not so accurate location only if it has decent overlap
                        distance < data.accuracy - placeData.radiusMeters
                    }

                    else -> false
                }
                val isEnter = (distance + accuracyThreshold) < placeData.radiusMeters && radiusMatch

                // user exited place if user's accuracy circle is not overlapping place circle
                // this is used to help filter location jumping when user stays in one closed place (like house)
                // in that case accuracy tends to worsen so in order to check for place exit we want to make sure user definitely left place
                // also adding few meters of buffer to improve filter
                val exitThreshold = when {
                    // TODO check for coarse location
                    data.accuracy > 500 -> {
                        // if accuracy is quite bad, only consider exit if user is very far away
                        2000f
                    }

                    data.accuracy > 100 -> {
                        // if accuracy is not that great, only consider exit if user is  somewhat far away
                        if (appPrefs.placeActivityThrottle) data.accuracy else data.accuracy / 2
                    }

                    else -> {
                        // if accuracy is decent,
                        if (appPrefs.placeActivityThrottle) (data.accuracy / 10).coerceAtLeast(5f) else 0f
                    }
                }
                val isExit = distance > placeData.radiusMeters + data.accuracy + exitThreshold
                val previousState = placeActivityRepo.getPlaceActivity(placeId = place.id)

//                logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

                if (isEnter) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupOrSpaceId,
                        enter = true,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                } else if (isExit) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupOrSpaceId,
                        enter = false,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                } else {
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
            val location =
                oneTimeLocation.getLastLocation(requestType = IOneTimeLocationProvider.RequestType.CacheOnly)
            if (location == null || location.accuracy.isLocationInaccurate()) {
                placeActivityRepo.updatePlaceActivity(placeId = placeId, state = null)
                logg.tag("place_activity")
                    .d("Cancel place $activityTag notify (location not accurate) - $placeId")
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
            val location =
                oneTimeLocation.getLastLocation(IOneTimeLocationProvider.RequestType.CacheOnly)
            location == null || location.accuracy.isLocationInaccurate() || location.timeUTCInstant?.isOlderThan(
                5.minutes
            ) != false
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

    private fun placeActivityDelay(
        place: GroupPlace?,
        trigger: PlaceActivityTriggerType
    ): Duration? {
        if (place == null) {
            return null
        }

        return when (trigger) {
            PlaceActivityTriggerType.GOEFENCE -> {
                when (place.alertSensitivity.sensitivityDelay) {
                    PlaceSensitivityDelayType.NONE -> null
                    PlaceSensitivityDelayType.MINIMUM -> 6.seconds
                    PlaceSensitivityDelayType.BALANCED -> 12.seconds
                    PlaceSensitivityDelayType.LONG -> 20.seconds
                }
            }

            else -> {
                when (place.alertSensitivity.sensitivityDelay) {
                    PlaceSensitivityDelayType.NONE -> 5.seconds
                    PlaceSensitivityDelayType.MINIMUM -> 10.seconds
                    PlaceSensitivityDelayType.BALANCED -> 15.seconds
                    PlaceSensitivityDelayType.LONG -> 25.seconds
                }
            }
        }
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