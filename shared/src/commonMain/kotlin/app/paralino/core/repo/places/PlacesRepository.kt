package app.paralino.core.repo.places

import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.models.places.GroupPlaceSettings
import app.paralino.core.models.places.GroupPlaceSettingsData
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class PlacesRepository constructor(
    private val local: PlacesLocalDataSource,
    private val remote: PlacesRemoteSource,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val userRepository: UserRepository,
    private val locationSharingManager: LocationSharingManager,
    private val geofenceManager: IGeofenceManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfig
) : BaseRepository() {

    private val placeActivity = MutableStateFlow(mapOf<String, PlaceActivityState>())

    fun hasAnyPlace(): Flow<Boolean> {
        return local.getNumberOfPlaces().map { it > 0 }
    }

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
            localUpdate = { local.recreatePlacesForGroup(groupId, it) }
        )
    }

    suspend fun refreshPlacesForGroup(groupId: String): List<GroupPlacesWithSettingsRaw> {
        return triggerRemoteOnly {
            remote.getPlacesForGroup(groupId)
        }.also { local.recreatePlacesForGroup(groupId, it) }
    }

    suspend fun refreshAllPlaces(): List<GroupPlacesWithSettingsRaw> {
        return triggerRemoteOnly { remote.getAllPlaces() }.also {
            local.recreatePlaces(it)
        }
    }

    suspend fun refreshPlace(placeId: String): GroupPlacesWithSettingsRaw {
        return triggerRemoteOnly { remote.getPlaceWithSettings(placeId) }.also {
            local.updateOrCreatePlace(it)
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
                updatePlaceActivity(placeId = remotePlace.id, state = null)
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

        val entered = mutableListOf<String>()
        val exited = mutableListOf<String>()

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
                val isEnter = distance < placeData.radiusMeters

                // user exited place if user's accuracy circle is not overlapping place circle
                // this is used to help filter location jumping when user stays in one closed place (like house)
                // in that case accuracy tends to worsen so in order to check for place exit we want to make sure user definitely left place
                val isExit = distance > placeData.radiusMeters + data.accuracy
                val previousState = placeActivity.value.get(place.id)

//                logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

                if (isEnter) entered.add(place.id)
                else if (isExit) exited.add(place.id)
                else {
                    // else -> there is no certain change to place activity, or it is the same as previous one
                    if (previousState?.enter == true)
                        entered.add(place.id)
                    else if (previousState?.enter == false)
                        exited.add(place.id)
                }
            }
        }

        placesEnter(entered, fromGeofence = false)
        placesExit(exited, fromGeofence = false)
    }

    fun placesEnter(placesIds: List<String>, fromGeofence: Boolean) {
        placesIds.forEach {
            try {
                schedulePlaceNotify(it, enter = true, fromGeofence = fromGeofence)
            } catch (e: Exception) {
                logg.e("place_activity", "Error notifying place enter", e)
            }
        }
    }

    fun placesExit(placesIds: List<String>, fromGeofence: Boolean) {
        placesIds.forEach {
            try {
                schedulePlaceNotify(it, enter = false, fromGeofence = fromGeofence)
            } catch (e: Exception) {
                logg.e("place_activity", "Error notifying place exit", e)
            }
        }
    }

    private fun schedulePlaceNotify(
        placeId: String,
        enter: Boolean,
        fromGeofence: Boolean
    ) {

        val sourceTag = if (fromGeofence) "geofence" else "location"
        val activityTag = if (enter) "${sourceTag}_enter" else "${sourceTag}_exit"

        val oldState = placeActivity.value.get(placeId)
        val newState = (oldState ?: PlaceActivityState(enter = enter)).copy(enter = enter)
        updatePlaceActivity(
            placeId = placeId,
            state = newState
        )

        if (newState.enter == oldState?.scheduledState || newState.enter == oldState?.enter) {
//            logg.tag("place_activity").d("Cancel place $activityTag notify - state did not change - $placeId")
            return
        }

        if (oldState?.tryNotifyJob?.isActive == true) {
//            logg.tag("place_activity").d("Place notify already scheduled $activityTag - $placeId")
            return
        }

        if (oldState == null || (oldState.enter == enter && oldState.lastNotifyState != !enter)) {
//            logg.tag("place_activity").d("Cancel place $activityTag notify (state: ${oldState?.enter}) - $placeId")
            return
        }

        logg.tag("place_activity").d("Schedule place notify $activityTag - $placeId")
        oldState.tryNotifyJob?.cancel()
        val updateJob = scopeProvider.userSessionIOScope.launch {
            if (appConfig.placeActivityThrottle) {
                delay(12.seconds)
            } else {
                delay(4.seconds)
            }
            processPlaceActivity(
                activityTag = activityTag,
                placeId = placeId,
                fromGeofence = fromGeofence
            )
        }

        updatePlaceActivity(
            placeId = placeId,
            state = newState.copy(scheduledState = enter, tryNotifyJob = updateJob)
        )
    }

    private suspend fun processPlaceActivity(
        activityTag: String,
        placeId: String,
        fromGeofence: Boolean
    ) {
        val currentState = placeActivity.value.get(placeId)

        if (currentState == null) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify - state is null - $placeId")
            return
        }

        if (currentState.enter != currentState.scheduledState) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify - state does not match - $placeId")
            updatePlaceActivity(
                placeId = placeId,
                state = currentState.copy(scheduledState = null)
            )
            return
        }

        val notifyTimeDiff =
            currentState.notifyTimeMillis.minus(nowUTC().toEpochMilliseconds()).absoluteValue

        // already notified this place in last period - this will filter frequent updates
        // but if user stays more than certain period inside the space, it will trigger notification
        if (notifyTimeDiff < 10.seconds.inWholeMilliseconds) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (notify diff) - $placeId")
            updatePlaceActivity(
                placeId = placeId,
                state = currentState.copy(scheduledState = null)
            )
            return
        }

        val place = local.getPlaceWithoutSettingsOnce(placeId)

        if (place == null) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (place null) - $placeId")
            updatePlaceActivity(
                placeId = placeId,
                state = null
            )
            return
        }

        if (!locationSharingManager.isSharingActiveForGroup(place.groupId)) {
            // reset place activity if sharing stops
            updatePlaceActivity(placeId, null)
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (sharing off) - $placeId")
            return
        }

        logg.tag("place_activity").d("Trigger notify place $activityTag - $placeId")
        updatePlaceActivity(
            placeId = placeId,
            state = currentState.copy(
                lastNotifyState = currentState.enter,
                notifyTimeMillis = nowUTC().toEpochMilliseconds(),
                scheduledState = null
            )
        )

        try {
            triggerRemoteOnly {
                if (currentState.enter) {
                    remote.notifyPlaceEnter(placeId = placeId, groupId = place.groupId)
                } else {
                    remote.notifyPlaceExit(placeId = placeId, groupId = place.groupId)
                }
            }

            // update device status with last location if place activity came from geofence
            if (fromGeofence) {
                try {
                    deviceStatusProcessor.pushDeviceStatus(groupId = place.groupId)
                } catch (e: Exception) {
                    logg.tag("place_activity").w("Error updating member status in ${place.groupId} from $activityTag - $placeId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            updatePlaceActivity(placeId, null)
            throw e
        }
    }

    private fun updatePlaceActivity(placeId: String, state: PlaceActivityState?) {
        placeActivity.update {
            it.toMutableMap().apply {
                if (state != null) {
                    set(placeId, state)
                } else {
                    remove(placeId).also {
                        it?.tryNotifyJob?.cancel()
                    }
                }
            }
        }
    }

    fun deletePlace(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deletePlace(id) },
            localUpdate = {
                local.deletePlace(id)
                updatePlaceActivity(id, state = null)
            }
        )
    }

    suspend fun clear() {
        local.clearAll()
        placeActivity.update {
            it.forEach { it.value.tryNotifyJob?.cancel() }
            emptyMap()
        }
    }

    suspend fun deleteLocallyForGroup(groupId: String) {
        local.deletePlaceForGroup(groupId)
    }
}

data class PlaceActivityState(
    val enter: Boolean,
    val scheduledState: Boolean? = null,
    val lastNotifyState: Boolean? = null,
    val notifyTimeMillis: Long = 0L,
    val tryNotifyJob: Job? = null,
)