package app.paralino.android.data.repos.places

import app.paralino.android.data.mappers.GroupPlaceMapperLocal
import app.paralino.android.data.mappers.GroupPlaceSettingsMapperLocal
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.repo.places.IPlacesLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class GroupPlacesLocalDataSource(
    private val dao: GroupPlacesDao,
    private val mapper: GroupPlaceMapperLocal,
    private val settingsMapper: GroupPlaceSettingsMapperLocal
): IPlacesLocalDataSource {

    override suspend fun recreatePlaces(places: List<GroupPlacesWithSettingsRaw>) {
        dao.replacePlaces(
            places = places.map { mapper.remote2Local(it.place) },
            settings = places.mapNotNull { it.settings?.let { settingsMapper.remote2Local(it) } }
        )
    }

    override suspend fun updateOrCreatePlace(place: GroupPlacesWithSettingsRaw) {
        dao.createOrUpdate(
            place = mapper.remote2Local(place.place),
            settings = place.settings?.let { settingsMapper.remote2Local(it) }
        )
    }

    override suspend fun recreatePlacesForGroup(
        groupId: String,
        places: List<GroupPlacesWithSettingsRaw>
    ) {
        dao.replacePlacesForGroup(
            groupId = groupId,
            places = places.map { mapper.remote2Local(it.place) },
            settings = places.mapNotNull { it.settings?.let { settingsMapper.remote2Local(it) } }
        )
    }

    override suspend fun updatePlaceOnly(place: GroupPlaceRaw) {
        dao.createOrUpdate(mapper.remote2Local(place))
    }

    override suspend fun updatePlaceSettings(settings: GroupPlaceSettingRaw) {
        dao.createOrUpdate(settingsMapper.remote2Local(settings))
    }

    override fun getPlaceRawPresentation(placeId: String): Flow<Pair<GroupPlaceRaw, GroupPlace>> {
        return dao.getPlace(placeId)
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    override fun getPlaceWithoutSettings(placeId: String): Flow<GroupPlace> {
        return dao.getPlace(placeId)
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Core(it) }
    }

    override fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings> {
        return combine(
            dao.getPlace(placeId),
            dao.getSettingsForPlace(placeId)
        ) { place, settings ->
            if(place != null) {
                GroupPlaceWithSettings(
                    place = mapper.local2Core(place),
                    settings = settings?.let { settingsMapper.local2Core(it) }
                )
            } else null
        }
            .filterNotNull()
    }

    override fun getPlacesForGroup(groupId: String): Flow<List<GroupPlace>> {
        return dao.getPlacesForGroup(groupId)
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
            }
    }

    override fun getAllPlaces(): Flow<List<GroupPlace>> {
        return dao.getAllPlaces()
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
            }
    }

    override fun getNumberOfPlaces(): Flow<Int> {
        return dao.getNumberOfPlaces()
    }

    override suspend fun getPlaceWithoutSettingsOnce(placeId: String): GroupPlace? {
        return dao.getPlaceOnce(placeId)?.let { mapper.local2Core(it) }
    }

    override suspend fun deletePlace(placeId: String) {
        dao.deletePlace(placeId)
    }

    override suspend fun deletePlaceForGroup(groupId: String) {
        dao.deletePlaceForGroup(groupId)
    }

    override suspend fun removeSetting(placeId: String, settingId: String) {
        dao.deletePlaceSettingsForPlaceId(placeId)
    }

    override suspend fun clearAll() {
        dao.clearPlaces()
    }

}