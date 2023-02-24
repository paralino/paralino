package app.paralino.core.repo.places

import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class GroupPlacesLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: GroupPlaceMapperLocal,
): IPlacesLocalDataSource {

    override suspend fun recreatePlaces(places: List<GroupPlacesWithSettingsRaw>) {
        database.transaction {
            placesQueries.clearAll()
            placeSettingsQueries.clearAll()
            places.forEach {
                placesQueries.createPlace(mapper.remote2Local(it.place))
                it.settings?.let { mapper.remote2Local(it) }?.let {
                    placeSettingsQueries.createPlaceSettings(it)
                }
            }
        }
    }

    override suspend fun updateOrCreatePlace(place: GroupPlacesWithSettingsRaw) {
        database.transaction {
            placesQueries.createPlace(mapper.remote2Local(place.place))
            if(place.settings == null)
                placeSettingsQueries.deleteForPlace(placeId = place.place.id)
            else
                placeSettingsQueries.createPlaceSettings(mapper.remote2Local(place.settings!!))
        }
    }

    override suspend fun recreatePlacesForGroup(
        groupId: String,
        places: List<GroupPlacesWithSettingsRaw>
    ) {
        database.transaction {
            placesQueries.deleteForGroup(groupId)
            placeSettingsQueries.deleteForGroup(groupId)
            places.forEach {
                placesQueries.createPlace(mapper.remote2Local(it.place))
                it.settings?.let { mapper.remote2Local(it) }?.let {
                    placeSettingsQueries.createPlaceSettings(it)
                }
            }
        }
    }

    override suspend fun updatePlaceOnly(place: GroupPlaceRaw) {
        database.placesQueries.createPlace(mapper.remote2Local(place))
    }

    override suspend fun updatePlaceSettings(settings: GroupPlaceSettingRaw) {
        database.placeSettingsQueries.createPlaceSettings(mapper.remote2Local(settings))
    }

    override fun getPlaceRawPresentation(placeId: String): Flow<Pair<GroupPlaceRaw, GroupPlace>> {
        return database.mapToOneOrNull { placesQueries.getPlace(placeId) }
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    override fun getPlaceWithoutSettings(placeId: String): Flow<GroupPlace> {
        return database.mapToOneOrNull { placesQueries.getPlace(placeId) }
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Core(it) }
    }

    override fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings> {
        return combine(
            database.mapToOneOrNull { placesQueries.getPlace(placeId) },
            database.mapToOneOrNull { placeSettingsQueries.getSettingsForPlace(placeId) }
        ) { place, settings ->
            if(place != null) {
                GroupPlaceWithSettings(
                    place = mapper.local2Core(place),
                    settings = settings?.let { mapper.local2Core(it) }
                )
            } else null
        }
            .filterNotNull()
    }

    override fun getPlacesForGroup(groupId: String): Flow<List<GroupPlace>> {
        return database.mapToList { placesQueries.getAllForGroup(groupId) }
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
            }
    }

    override fun getAllPlaces(): Flow<List<GroupPlace>> {
        return database.mapToList { placesQueries.getAll() }
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
            }
    }

    override fun getNumberOfPlaces(): Flow<Int> {
        return database.mapToOneOrNull { placesQueries.getPlacesCount() }
            .filterNotNull()
            .map { it.toInt() }
    }

    override suspend fun getPlaceWithoutSettingsOnce(placeId: String): GroupPlace? {
        return database.placesQueries.getPlace(placeId).executeAsOneOrNull()?.let { mapper.local2Core(it) }
    }

    override suspend fun deletePlace(placeId: String) {
        database.transaction {
            placesQueries.deletePlace(placeId)
            placeSettingsQueries.deleteForPlace(placeId)
        }
    }

    override suspend fun deletePlaceForGroup(groupId: String) {
        database.transaction {
            placesQueries.deleteForGroup(groupId)
            placeSettingsQueries.deleteForGroup(groupId)
        }
    }

    override suspend fun clearAll() {
        database.transaction {
            placesQueries.clearAll()
            placeSettingsQueries.clearAll()
        }
    }
}