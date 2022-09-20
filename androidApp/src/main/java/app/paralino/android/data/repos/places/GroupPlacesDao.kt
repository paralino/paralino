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
    suspend fun createOrUpdate(place: GroupPlaceEntity, settings: GroupPlaceSettingsEntity) {
        createOrUpdate(place)
        createOrUpdate(settings)
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

    @Query("SELECT * FROM place_settings WHERE placeId = :placeId")
    fun getSettingsForPlace(placeId: String): Flow<GroupPlaceSettingsEntity?>

    @Transaction
    suspend fun deletePlace(id: String) {
        deletePlaceOnly(id)
        deletePlaceSettingsForPlaceId(id)
    }

    @Query("DELETE FROM group_places WHERE id = :id")
    suspend fun deletePlaceOnly(id: String)

    @Query("DELETE FROM place_settings WHERE placeId = :placeId")
    suspend fun deletePlaceSettingsForPlaceId(placeId: String)

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