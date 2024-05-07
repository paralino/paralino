package app.paralino.core.repo.places

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.data
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPlaceSettingRaw
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class PlacesLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: GroupPlaceMapperLocal,
) {

    suspend fun recreatePlaces(places: List<GroupPlacesWithSettingsRaw>) {
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

    suspend fun updateOrCreatePlace(place: GroupPlacesWithSettingsRaw) {
        database.transaction {
            placesQueries.createPlace(mapper.remote2Local(place.place))
            if (place.settings == null)
                placeSettingsQueries.deleteForPlace(placeId = place.place.id)
            else
                placeSettingsQueries.createPlaceSettings(mapper.remote2Local(place.settings!!))
        }
    }

    suspend fun recreatePlacesForGroup(
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

    suspend fun updatePlaceOnly(place: GroupPlaceRaw) {
        database.placesQueries.createPlace(mapper.remote2Local(place))
    }

    suspend fun updatePlaceSettings(settings: GroupPlaceSettingRaw) {
        database.placeSettingsQueries.createPlaceSettings(mapper.remote2Local(settings))
    }

    fun getPlaceRawPresentation(placeId: String): Flow<Pair<GroupPlaceRaw, GroupPlace>> {
        return database.mapToOneOrNull { placesQueries.getPlace(placeId) }
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    fun getPlaceWithoutSettings(placeId: String): Flow<GroupPlace> {
        return database.mapToOneOrNull { placesQueries.getPlace(placeId) }
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Core(it) }
    }

    fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings> {
        return combine(
            database.mapToOneOrNull { placesQueries.getPlace(placeId) },
            database.mapToOneOrNull { placeSettingsQueries.getSettingsForPlace(placeId) }
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
        return database.mapToList { placesQueries.getAllForGroup(groupId) }
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
                    .sortedBy { it.data.data?.name }
            }
    }

    fun getAllPlaces(): Flow<List<GroupPlace>> {
        return database.mapToList { placesQueries.getAll() }
            .distinctFlowOfList()
            .map {
                it.map { mapper.local2Core(it) }
            }
    }

    suspend fun getAllPlacesOnce(): List<GroupPlace> {
        return database.placesQueries
            .getAll()
            .executeAsList()
            .map { mapper.local2Core(it) }
    }

    fun getNumberOfPlaces(): Flow<Int> {
        return database.mapToOneOrNull { placesQueries.getPlacesCount() }
            .filterNotNull()
            .map { it.toInt() }
    }

    suspend fun getPlaceWithoutSettingsOnce(placeId: String): GroupPlace? {
        return database.placesQueries.getPlace(placeId).executeAsOneOrNull()
            ?.let { mapper.local2Core(it) }
    }

    suspend fun getGroupId(placeId: String): String? {
        return database.executeWithResult {
            placesQueries.getGroupIdForPlaceId(placeId = placeId).executeAsOneOrNull()
        }
    }
}