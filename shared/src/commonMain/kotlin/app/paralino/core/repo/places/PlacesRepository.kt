package app.paralino.core.repo.places

import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.places.*
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.*
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class PlacesRepository constructor(
    private val local: PlacesLocalDataSource,
    private val remote: PlacesRemoteSource,
    private val userRepository: UserRepository,
    private val locationSharingManager: LocationSharingManager,
    private val geofenceManager: IGeofenceManager,
) : BaseRepository() {

    // TODO maybe clear place activity if location sharing stops?
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
            localUpdate = { key ->
                local.updatePlaceOnly(key)
                placeActivity.update { it.toMutableMap().apply { remove(key.id) } }
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

                logg.tag("place_activity").d("Process place activity (enter: $isEnter - exit: $isExit - previous: ${previousState?.enter}) - ${place.id}")

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

        placesEnter(entered)
        placesExit(exited)
    }

    suspend fun placesEnter(placesIds: List<String>) {
        placesIds.forEach {
            try {
                processPlaceActivity(it, enter = true)
            } catch (e: Exception) {
                logg.e("Error notifying place enter", e)
            }
        }
    }

    suspend fun placesExit(placesIds: List<String>) {
        placesIds.forEach {
            try {
                processPlaceActivity(it, enter = false)
            } catch (e: Exception) {
                logg.e("Error notifying place exit", e)
            }
        }
    }

    private suspend fun processPlaceActivity(placeId: String, enter: Boolean): Boolean {

        val activityTag = if (enter) "enter" else "exit"
//        logg.tag("place_activity").d("Try to notify place $activityTag - $placeId")

        val previousState = placeActivity.value.get(placeId)
        val newState = (previousState ?: PlaceActivityState())
            .copy(enter = enter, activityTimeMillis = nowUTC().toEpochMilliseconds())

        updatePlaceActivity(placeId, newState)

        // never entered/exited or already out/inside this place
        // only take into account if didn't notify it before
        if (previousState == null || (previousState.enter == enter && previousState.lastNotifyState != !enter)) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (state: ${previousState?.enter}) - $placeId")
            return false
        }

        val notifyTimeDiff =
            previousState.notifyTimeMillis.minus(nowUTC().toEpochMilliseconds()).absoluteValue

        // already notified this place in last period - this will filter frequent updates
        // but if user stays more than certain period inside the space, it will trigger notification
        if (notifyTimeDiff < 10.seconds.inWholeMilliseconds) {
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (notify diff) - $placeId")
            return false
        }

        val place = local.getPlaceWithoutSettingsOnce(placeId)

        if (place == null) {
            logg.tag("place_activity").d("Cancel place $activityTag notify (place null) - $placeId")
            return false
        }

        if (!locationSharingManager.isSharingActiveForGroup(place.groupId)) {
            // reset place activity if sharing stops
            updatePlaceActivity(placeId, null)
            logg.tag("place_activity")
                .d("Cancel place $activityTag notify (sharing off) - $placeId")
            return false
        }

        logg.tag("place_activity").d("Trigger notify place $activityTag - $placeId")
        updatePlaceActivity(
            placeId,
            newState.copy(lastNotifyState = enter, notifyTimeMillis = nowUTC().toEpochMilliseconds())
        )
        try {
            triggerRemoteOnly {
                if (enter) {
                    remote.notifyPlaceEnter(placeId)
                } else {
                    remote.notifyPlaceExit(placeId)
                }
            }
        } catch (e: Exception) {
            updatePlaceActivity(placeId, null)
            throw e
        }

        return true
    }

    private fun updatePlaceActivity(placeId: String, state: PlaceActivityState?) {
        placeActivity.update {
            it.toMutableMap().apply {
                if (state != null)
                    set(placeId, state)
                else
                    remove(placeId)
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
        placeActivity.update { emptyMap() }
    }

    suspend fun deleteLocallyForGroup(groupId: String) {
        local.deletePlaceForGroup(groupId)
    }

}

data class PlaceActivityState(
    val enter: Boolean = true,
    val lastNotifyState: Boolean? = null,
    val activityTimeMillis: Long = 0L,
    val notifyTimeMillis: Long = 0L,
)