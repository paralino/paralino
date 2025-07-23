package app.paralino.core.repo.places

import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceData
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlacesRepository constructor(
    private val local: PlacesLocalDataSource,
    private val remote: PlacesRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val placeActivityRepo: PlaceActivityDataSource,

) : BaseRepository() {

    fun getAllPlacesLocally(): Flow<List<GroupPlace>> {
        return local.getAllPlaces()
    }

    fun getPlacesForGroupLocally(groupId: String): Flow<List<GroupPlace>> {
        return local.getPlacesForGroup(groupId)
    }

    fun getPlaceWithSettingsLocally(placeId: String): Flow<GroupPlaceWithSettings> {
        return local.getPlaceWithSettings(placeId)
    }

    suspend fun refreshPlaceWithSettingsMaybe(placeId: String) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "place_detail_$placeId",
                timeMillis = 10000
            )
        ) {
            remote.getPlaceWithSettings(placeId = placeId)
                .also { groupLocal.updateOrCreatePlace(it) }
        }
    }

    fun getAndRefreshPlacesForGroup(groupId: String): Flow<List<GroupPlace>> {
        return mergeDataWithRemote(
            local = local.getPlacesForGroup(groupId),
            remote = {
                remote.getPlacesForGroup(groupId)
            },
            localUpdate = {
                groupLocal.recreatePlacesForGroup(groupId, it)
            }
        )
    }

    fun createPlace(
        groupId: String,
        private: Boolean,
        isInPersonalSpace: Boolean,
        data: GroupPlaceData
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.createPlace(
                    groupId = groupId,
                    data = data,
                    isInPersonalSpace = isInPersonalSpace,
                    private = private
                )
            },
            localUpdate = { groupLocal.updateOrCreatePlace(it) }
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