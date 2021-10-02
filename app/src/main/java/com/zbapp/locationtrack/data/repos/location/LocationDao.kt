package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Transaction
    suspend fun saveOnlyOneLocation(vararg data: LocationEntity) {
        data.forEach {
            clearForUser(it.userId)
        }
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: LocationEntity)

    @Query("SELECT * FROM locations WHERE userId IN (:userIds)")
    fun getLocationsForUsers(userIds: List<String>): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE userId = :userId")
    fun getLastLocationForUser(userId: String): Flow<LocationEntity>

    @Query("DELETE FROM locations WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM locations")
    suspend fun clear()


}