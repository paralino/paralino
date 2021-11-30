package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Transaction
    suspend fun saveOnlyOneLocation(vararg data: MemberEntity) {
        data.forEach {
            clearForUser(it.userId)
        }
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: MemberEntity)

    @Query("SELECT * FROM members WHERE userId IN (:userIds)")
    fun getLocationsForUsers(userIds: List<String>): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE userId = :userId")
    fun getLastLocationForUser(userId: String): Flow<MemberEntity>

    @Query("DELETE FROM members WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM members")
    suspend fun clear()


}