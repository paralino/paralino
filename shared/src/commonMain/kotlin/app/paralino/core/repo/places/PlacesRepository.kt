package app.paralino.core.repo.places

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
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
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val mapper: PlaceMapperCore,
): BaseRepository() {

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

    suspend fun refreshAndGetPlacesForGroup(groupId: String): Flow<List<GroupPlacesWithSettingsRaw>> {
        return getDataOnce(localUpdate = { local.recreatePlacesForGroup(groupId, it) }) { remote.getPlacesForGroup(groupId) }
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