package app.paralino.core.repo.places

import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class PlacesRepository constructor(
    private val local: PlacesLocalDataSource,
    private val remote: PlacesRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val placeActivityUploadEngine: PlaceActivityUploadEngine,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val userRepository: UserRepository,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val geofenceManager: IGeofenceManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfig
) : BaseRepository() {

    fun subscribeToAllActivePlaces(): Flow<List<GroupPlace>> {
        return local.getAllPlaces().map {
            it.filter { locationSharingManager.isSharingActiveForGroup(it.groupId) }
        }
    }

    suspend fun getAllActivePlaces(): List<GroupPlace> {
        return local.getAllPlacesOnce().filter {
            locationSharingManager.isSharingActiveForGroup(it.groupId)
        }
    }

    fun getPlacesForGroupLocally(groupId: String): Flow<List<GroupPlace>> {
        return local.getPlacesForGroup(groupId)
    }

    fun getPlaceLocally(placeId: String): Flow<GroupPlace> {
        return local.getPlaceWithoutSettings(placeId)
    }

    suspend fun getPlaceLocallyOnce(placeId: String): GroupPlace? {
        return local.getPlaceWithoutSettingsOnce(placeId)
    }

    fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings> {
        return mergeDataWithRemote(
            local = local.getPlaceWithSettings(placeId),
            remote = {
                remote.getPlaceWithSettings(placeId)
            },
            localUpdate = { local.updateOrCreatePlace(it) },
            remotePreference = RemotePreference.CACHE(key = "place_detail", timeMillis = 5_000)
        )
    }

    fun getAndRefreshPlacesForGroup(groupId: String): Flow<List<GroupPlace>> {
        return mergeDataWithRemote(
            local = local.getPlacesForGroup(groupId),
            remote = {
                remote.getPlacesForGroup(groupId)
            },
            localUpdate = {
                local.recreatePlacesForGroup(groupId, it)
                placeActivityRepo.recreatePlacesForGroup(groupId, it)
            }
        )
    }

    suspend fun refreshPlacesForGroup(groupId: String): List<GroupPlacesWithSettingsRaw> {
        return triggerRemoteOnly {
            remote.getPlacesForGroup(groupId)
        }.also {
            local.recreatePlacesForGroup(groupId, it)
            placeActivityRepo.recreatePlacesForGroup(groupId, it)
        }
    }

    suspend fun refreshAllPlaces(): List<GroupPlacesWithSettingsRaw> {
        return triggerRemoteOnly { remote.getAllPlaces() }.also {
            local.recreatePlaces(it)
            placeActivityRepo.recreatePlaces(it)
        }
    }

    fun createPlace(groupId: String, private: Boolean, data: GroupPlaceData): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.createPlace(
                    groupId = groupId,
                    data = data,
                    private = private
                )
            },
            localUpdate = { local.updateOrCreatePlace(it) }
        ).map { Unit }
    }

    fun updatePlace(place: GroupPlace): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updatePlace(place) },
            localUpdate = { remotePlace ->
                local.updatePlaceOnly(remotePlace)
                placeActivityRepo.updatePlaceActivity(placeId = remotePlace.id, state = null)
            }
        ).map { Unit }
    }

    suspend fun updatePlaceLocally(place: GroupPlaceRaw) {
        local.updatePlaceOnly(place)
    }

    fun updatePlaceSettings(
        placeId: String,
        data: GroupPlaceSettingsData
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val place = local.getPlaceWithSettings(placeId).first()
                val currentSettings = place.settings

                val settings = if (currentSettings != null) {
                    val currentAlert =
                        currentSettings.alertData.firstOrNull { it.memberUserId == data.memberUserId }

                    val newAlerts = if (currentAlert != null) {
                        currentSettings.alertData.toMutableList().apply {
                            removeAll { it.memberUserId == data.memberUserId }
                            add(data)
                        }
                    } else {
                        currentSettings.alertData.toMutableList().apply { add(data) }
                    }

                    currentSettings.copy(alertData = newAlerts)
                } else {
                    GroupPlaceSettings(
                        id = "",
                        groupId = place.place.groupId,
                        placeId = placeId,
                        ownerUserId = userRepository.cachedUserId,
                        alertData = listOf(data)
                    )
                }
                remote.updatePlaceSettings(settings)
            },
            localUpdate = {
                local.updatePlaceSettings(it)
            }
        ).map { Unit }
    }

    suspend fun processLocation(data: LocationDataModel) {
        // TODO filter out frequent calls and calculations of distance between points
        logg.v("place_activity", "Process new location in places")
        val places = getAllActivePlaces()

        places.forEach { place ->
            place.data.data?.let { placeData ->
                val distance = geofenceManager.distanceBetweenPoints(
                    data,
                    GroupPlaceGeofenceData(
                        groupId = place.groupId,
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
                val isExit =
                    distance > (placeData.radiusMeters + data.accuracy + if (appConfig.placeActivityThrottle) 5 else 0)
                val previousState = placeActivityRepo.getPlaceActivity(placeId = place.id)

//                logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

                if (isEnter) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupId,
                        enter = true,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                } else if (isExit) {
                    startPlaceNotifySchedule(
                        placeId = place.id,
                        groupId = place.groupId,
                        enter = false,
                        trigger = PlaceActivityTriggerType.LOCATION
                    )
                } else {
                    // else -> there is no certain change to place activity, or it is the same as previous one
                    if (previousState?.isEnter == true) {
                        startPlaceNotifySchedule(
                            placeId = place.id,
                            groupId = place.groupId,
                            enter = true,
                            trigger = PlaceActivityTriggerType.LOCATION
                        )
                    } else if (previousState?.isEnter == false) {
                        startPlaceNotifySchedule(
                            placeId = place.id,
                            groupId = place.groupId,
                            enter = false,
                            trigger = PlaceActivityTriggerType.LOCATION
                        )
                    }
                }
            }
        }
    }

    fun placesEnter(placesIds: List<String>, trigger: PlaceActivityTriggerType) {
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val groupId = local.getGroupId(placeId = placeId)!!
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
        scopeProvider.userSessionIOScope.launch {
            placesIds.forEach { placeId ->
                try {
                    val groupId = local.getGroupId(placeId = placeId)!!
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
        groupId: String,
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
        val updateJob = scopeProvider.userSessionIOScope.launch {
            placeActivityDelay()
            try {
                processPlaceActivity(
                    activityTag = activityTag,
                    placeId = placeId
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
        placeId: String
    ) {
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

        if (!locationSharingManager.isSharingActiveForGroup(place.groupId)) {
            // reset place activity if sharing stops
            placeActivityRepo.updatePlaceActivity(placeId, null)
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (sharing off) - $placeId")
            return
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
                placeId = placeId,
                groupId = place.groupId,
                isEnter = currentState.isEnter,
                timeMillis = currentState.stateChangeTimeMillis
            )

            // update device status with last location if place activity came from geofence
            if (currentState.scheduleTrigger == PlaceActivityTriggerType.GOEFENCE) {
                updateDeviceStatusAndLocation(
                    activityTag = activityTag,
                    place = place
                )
            }
        } catch (e: Exception) {
            placeActivityRepo.updatePlaceActivity(placeId, null)
            throw e
        }
    }

    private fun updateDeviceStatusAndLocation(
        activityTag: String,
        place: GroupPlace
    ) {
        scopeProvider.userSessionIOScope.launch {
            // update device status with last location if place activity came from geofence
            try {
                deviceStatusProcessor.pushDeviceStatus(groupId = place.groupId)
            } catch (e: Exception) {
                logg.tag("place_activity")
                    .w("Error updating member status in ${place.groupId} from $activityTag - ${place.id}: ${e.message}")
            }
        }
        locationPerformanceManager.locationBoostRequested(
            request = LocationPerformanceRequest(
                type = LocationPerformanceType.HIGH,
                durationMillis = 12.seconds.inWholeMilliseconds
            ),
            groupId = place.groupId,
            from = "place $activityTag"
        )
    }

    suspend fun placeActivityDelay() {
        if (appConfig.placeActivityThrottle) {
            delay(20.seconds)
        } else {
            delay(5.seconds)
        }
    }

    fun deletePlace(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deletePlace(id) },
            localUpdate = {
                groupLocal.deletePlace(id)
                placeActivityRepo.updatePlaceActivity(id, state = null)
            }
        )
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