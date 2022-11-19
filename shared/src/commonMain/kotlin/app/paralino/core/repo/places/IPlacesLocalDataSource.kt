package app.paralino.core.repo.places

import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.raw.*
import kotlinx.coroutines.flow.Flow

interface IPlacesLocalDataSource {

    suspend fun recreatePlaces(places: List<GroupPlacesWithSettingsRaw>)
    suspend fun recreatePlacesForGroup(groupId: String, places: List<GroupPlacesWithSettingsRaw>)
    suspend fun updateOrCreatePlace(place: GroupPlacesWithSettingsRaw)
    suspend fun updatePlaceOnly(place: GroupPlaceRaw)
    suspend fun updatePlaceSettings(settings: GroupPlaceSettingRaw)

    fun getPlaceRawPresentation(placeId: String): Flow<Pair<GroupPlaceRaw, GroupPlace>>
    fun getPlaceWithoutSettings(placeId: String): Flow<GroupPlace>
    fun getPlaceWithSettings(placeId: String): Flow<GroupPlaceWithSettings>
    fun getPlacesForGroup(groupId: String): Flow<List<GroupPlace>>
    fun getAllPlaces(): Flow<List<GroupPlace>>
    fun getNumberOfPlaces(): Flow<Int>
    suspend fun getPlaceWithoutSettingsOnce(placeId: String): GroupPlace?

    suspend fun deletePlace(placeId: String)
    suspend fun deletePlaceForGroup(groupId: String)
    suspend fun removeSetting(placeId: String, settingId: String)
    suspend fun clearAll()
}