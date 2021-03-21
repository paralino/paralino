package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Transaction
    suspend fun saveOnlyOneLocation(vararg data: LocationEntity) {
        data.forEach {
            clearForUser(it.userId, it.circleId)
        }
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: LocationEntity)

    @Query("SELECT * FROM locations WHERE userId = :userId")
    fun getLocationsForUser(userId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE circleId = :circleId")
    fun getLocationsForCircle(circleId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE userId = :userId ORDER BY time ASC LIMIT 1")
    fun getLastLocationForUser(userId: String): Flow<LocationEntity>

    @Query("DELETE FROM locations WHERE userId = :userId AND circleId = :circleId")
    suspend fun clearForUser(userId: String, circleId: String)

    @Query("DELETE FROM locations")
    suspend fun clear()


}