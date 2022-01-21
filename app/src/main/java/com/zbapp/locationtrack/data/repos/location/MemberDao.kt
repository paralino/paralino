package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Transaction
    suspend fun recreateForGroup(groupId: String, data: List<MemberEntity>) {
        clearAllMembersForGroup(groupId)
        createOrUpdate(*data.toTypedArray())
    }

    @Transaction
    suspend fun recreateAll(data: List<MemberEntity>) {
        clear()
        createOrUpdate(*data.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: MemberEntity)

    @Query("SELECT * FROM members WHERE userId IN (:userIds)")
    fun getLocationsForUsers(userIds: List<String>): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE userId = :userId")
    fun getLastLocationForUser(userId: String): Flow<MemberEntity>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getLocationsForGroup(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(:groupId) FROM members")
    fun memberCountInGroup(groupId: String): Flow<Int>

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun removeMember(id: String)

    @Query("DELETE FROM members WHERE userId = :userId")
    suspend fun clearAllMembersForUser(userId: String)

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun clearAllMembersForGroup(groupId: String)

    @Query("DELETE FROM members WHERE groupId = :groupId AND userId = :userId")
    suspend fun clearAllMembersForGroup(groupId: String, userId: String)

    @Query("DELETE FROM members")
    suspend fun clear()


}