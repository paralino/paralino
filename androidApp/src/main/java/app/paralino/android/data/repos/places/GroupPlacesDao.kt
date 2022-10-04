package app.paralino.android.data.repos.places

import androidx.room.*
import app.paralino.android.data.models.places.GroupPlaceEntity
import app.paralino.android.data.models.places.GroupPlaceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupPlacesDao {

    @Transaction
    suspend fun replacePlaces(places: List<GroupPlaceEntity>, settings: List<GroupPlaceSettingsEntity>) {
        clearPlaces()
        clearSettings()
        createOrUpdate(*places.toTypedArray())
        createOrUpdate(*settings.toTypedArray())
    }

    @Transaction
    suspend fun replacePlacesForGroup(groupId: String, places: List<GroupPlaceEntity>, settings: List<GroupPlaceSettingsEntity>) {
        deletePlaceForGroupId(groupId)
        deletePlaceSettingsForGroupId(groupId)
        createOrUpdate(*places.toTypedArray())
        createOrUpdate(*settings.toTypedArray())
    }

    @Transaction
    suspend fun createOrUpdate(place: GroupPlaceEntity, settings: GroupPlaceSettingsEntity?) {
        createOrUpdate(place)

        if(settings == null) {
            deletePlaceSettingsForPlaceId(place.id)
        } else {
            createOrUpdate(settings)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupPlaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupPlaceSettingsEntity)

    @Query("SELECT * FROM group_places")
    fun getAllPlaces(): Flow<List<GroupPlaceEntity>>

    @Query("SELECT * FROM place_settings")
    fun getAllPlaceSettings(): Flow<List<GroupPlaceSettingsEntity>>

    @Query("SELECT * FROM group_places WHERE id = :id")
    fun getPlace(id: String): Flow<GroupPlaceEntity?>

    @Query("SELECT * FROM group_places WHERE id = :id")
    suspend fun getPlaceOnce(id: String): GroupPlaceEntity?

    @Query("SELECT * FROM place_settings WHERE placeId = :placeId")
    fun getSettingsForPlace(placeId: String): Flow<GroupPlaceSettingsEntity?>

    @Query("SELECT * FROM group_places WHERE groupId = :groupId")
    fun getPlacesForGroup(groupId: String): Flow<List<GroupPlaceEntity>>

    @Transaction
    suspend fun deletePlace(id: String) {
        deletePlaceOnly(id)
        deletePlaceSettingsForPlaceId(id)
    }

    @Transaction
    suspend fun deletePlaceForGroup(groupId: String) {
        deletePlaceForGroupId(groupId)
        deletePlaceSettingsForGroupId(groupId)
    }

    @Query("DELETE FROM group_places WHERE id = :id")
    suspend fun deletePlaceOnly(id: String)

    @Query("DELETE FROM place_settings WHERE placeId = :placeId")
    suspend fun deletePlaceSettingsForPlaceId(placeId: String)

    @Query("DELETE FROM group_places WHERE groupId = :groupId")
    suspend fun deletePlaceForGroupId(groupId: String)

    @Query("DELETE FROM place_settings WHERE groupId = :groupId")
    suspend fun deletePlaceSettingsForGroupId(groupId: String)

    @Transaction
    suspend fun clearAll() {
        clearPlaces()
        clearSettings()
    }

    @Query("DELETE FROM group_places")
    suspend fun clearPlaces()

    @Query("DELETE FROM place_settings")
    suspend fun clearSettings()

}