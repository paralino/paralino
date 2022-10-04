package app.paralino.core.repo.places

import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.places.*
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PlacesRepository constructor(
    private val local: IPlacesLocalDataSource,
    private val remote: PlacesRemoteSource,
    private val userRepository: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val locationSharingManager: LocationSharingManager
) : BaseRepository() {

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
                    private = private,
                    myKey = userKeyRepo.latestKeyPair()
                )
            },
            localUpdate = { local.updateOrCreatePlace(it) }
        ).map { Unit }
    }

    fun updatePlace(place: GroupPlace): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updatePlace(place) },
            localUpdate = { local.updatePlaceOnly(it) }
        ).map { Unit }
    }

    suspend fun updatePlaceLocally(place: GroupPlaceRaw) {
        local.updatePlaceOnly(place)
    }

    fun updatePlaceSettings(
        placeId: String,
        memberId: String,
        data: GroupPlaceSettingsData
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                val place = local.getPlaceWithSettings(placeId).first()
                val currentSettings = place.settings

                val settings = if (currentSettings != null) {
                    val currentAlert =
                        currentSettings.alertData.firstOrNull { it.memberId == data.memberId }

                    val newAlerts = if (currentAlert != null) {
                        currentSettings.alertData.toMutableList().apply {
                            removeAll { it.memberId == data.memberId }
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
                        memberId = memberId,
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

    suspend fun placesEnter(placesIds: List<String>) {
        placesIds.forEach {
            try {
                placeEnter(it)
            } catch (e: Exception) {
                logg.e("Error notifying place enter", e)
            }
        }
    }

    suspend fun placesExit(placesIds: List<String>) {
        placesIds.forEach {
            try {
                placeExit(it)
            } catch (e: Exception) {
                logg.e("Error notifying place exit", e)
            }
        }
    }

    suspend fun placeEnter(placeId: String) {
        val place = local.getPlaceWithoutSettingsOnce(placeId) ?: return
        if (!locationSharingManager.isSharingActiveForGroup(place.groupId)) return

        triggerRemoteOnly {
            remote.notifyPlaceEnter(placeId)
        }
    }

    suspend fun placeExit(placeId: String) {
        val place = local.getPlaceWithoutSettingsOnce(placeId) ?: return
        if (!locationSharingManager.isSharingActiveForGroup(place.groupId)) return

        triggerRemoteOnly {
            remote.notifyPlaceExit(placeId)
        }
    }

    fun deletePlace(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deletePlace(id) },
            localUpdate = { local.deletePlace(id) }
        )
    }

    suspend fun clear() {
        local.clearAll()
    }

    suspend fun deleteLocallyForGroup(groupId: String) {
        local.deletePlaceForGroup(groupId)
    }

}