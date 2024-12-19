package app.paralino.core.repo.places

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.data
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class PlacesLocalDataSource(
    private val database: MainDatabaseHelper,
    private val mapper: GroupPlaceMapperLocal
) {

    suspend fun updatePlaceOnly(place: GroupPlaceRaw) {
        database.groupRelatedDao.createPlace(mapper.remote2Local(place))
    }

    suspend fun updatePlaceSettings(settings: GroupPlaceSettingRaw) {
        database.groupRelatedDao.createPlaceSettings(mapper.remote2Local(settings))
    }

    fun getPlaceRawPresentation(placeId: String): Flow<Pair<GroupPlaceRaw, GroupPlace>> {
        return database.groupRelatedDao.getPlace(placeId = placeId)
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    fun getPlaceWithoutSettings(placeId: String): Flow<GroupPlace> {
        return database.groupRelatedDao.getPlace(placeId = placeId)
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Core(it) }
    }

    fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings> {
        return combine(
            database.groupRelatedDao.getPlace(placeId = placeId),
            database.groupRelatedDao.getSettingsForPlace(placeId = placeId)
        ) { place, settings ->
            if (place != null) {
                GroupPlaceWithSettings(
                    place = mapper.local2Core(place),
                    settings = settings?.let { mapper.local2Core(it) }
                )
            } else null
        }
            .filterNotNull()
    }

    fun getPlacesForGroup(groupId: String): Flow<List<GroupPlace>> {
        return database.groupRelatedDao.getAllPlacesForGroup(groupId = groupId)
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
                    .sortedBy { it.data.data?.name }
            }
    }

    fun getAllPlaces(): Flow<List<GroupPlace>> {
        return database.groupRelatedDao.getAllPlaces()
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
                    .sortedBy { it.id }
            }
    }

    suspend fun getAllPlacesOnce(): List<GroupPlace> {
        return database.groupRelatedDao
            .getAllPlacesOnce()
            .map { mapper.local2Core(it) }
    }

    suspend fun getPlaceWithoutSettingsOnce(placeId: String): GroupPlace? {
        return database.groupRelatedDao.getPlaceOnce(placeId = placeId)
            ?.let { mapper.local2Core(it) }
    }

    suspend fun getGroupId(placeId: String): String? {
        return database.groupRelatedDao.getGroupIdForPlace(placeId = placeId)
    }

    suspend fun getPlacesCountForGroupOnce(groupId: String): Int? {
        return database.groupRelatedDao.getPlacesCountForGroupOnce(groupId)
    }
}