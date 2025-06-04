package app.paralino.core.db.main.places

import androidx.room.*

@Dao
interface PlaceActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaceActivity(activity: PlaceActivityEntity)

    @Query("SELECT * FROM placeActivityEntity")
    suspend fun getAll(): List<PlaceActivityEntity>

    @Query("SELECT * FROM placeActivityEntity WHERE groupId = :groupId")
    suspend fun getAllForGroup(groupId: String): List<PlaceActivityEntity>


    @Transaction
    suspend fun clearMissing(groupId: String, placeIds: List<String>) {
        val current = getAllForGroup(groupId = groupId)
        current.forEach { activity ->
            if (placeIds.none { it == activity.placeId }) {
                deletePlaceActivity(placeId = activity.placeId)
            }
        }
    }

    @Transaction
    suspend fun clearMissing(placeIds: List<String>) {
        val current = getAll()
        current.forEach { activity ->
            if (placeIds.none { it == activity.placeId }) {
                deletePlaceActivity(placeId = activity.placeId)
            }
        }
    }


    @Query("DELETE FROM placeActivityEntity WHERE placeId = :placeId")
    suspend fun deletePlaceActivity(placeId: String)

    @Query("DELETE FROM placeActivityEntity WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)
}
