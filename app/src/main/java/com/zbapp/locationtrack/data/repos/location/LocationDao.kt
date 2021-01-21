package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.LocationModel
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Transaction
    suspend fun saveOnlyOneLocation(data: LocationModel) {
        clearForUser(data.userId)
        createOrUpdate(data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: LocationModel)

    @Query("SELECT * FROM locations WHERE userId = :userId")
    suspend fun getLocationsForUser(userId: String): Flow<List<LocationModel>>

    @Query("SELECT * FROM locations ORDER BY time ASC LIMIT 1")
    suspend fun getLastLocationForUser(userId: String): Flow<LocationModel>

    @Query("DELETE FROM locations WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM locations")
    suspend fun clear()


}