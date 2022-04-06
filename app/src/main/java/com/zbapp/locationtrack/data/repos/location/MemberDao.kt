package com.zbapp.locationtrack.data.repos.location

import androidx.room.*
import com.zbapp.locationtrack.data.models.location.MemberEntity
import com.zbapp.locationtrack.data.models.location.MemberMutableDataUpdate
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

    @Query("SELECT COUNT(:groupId) FROM members WHERE groupId = :groupId")
    suspend fun memberCountInGroup(groupId: String): Int

    @Query("SELECT id FROM members WHERE userId = :userId AND groupId = :groupId")
    suspend fun getUserMemberIds(userId: String, groupId: String): List<String>

    @Query("SELECT ownerPublicKey FROM members WHERE groupId = :groupId AND userId = :userId")
    suspend fun getMemberPublicKeyOnce(groupId: String, userId: String): String?

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun removeMember(id: String)

    @Update(onConflict = OnConflictStrategy.IGNORE, entity = MemberEntity::class)
    suspend fun updateMemberMutableData(data: MemberMutableDataUpdate)

    @Query("DELETE FROM members WHERE userId = :userId")
    suspend fun clearAllMembersForUser(userId: String)

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun clearAllMembersForGroup(groupId: String)

    @Query("DELETE FROM members WHERE groupId = :groupId AND userId = :userId")
    suspend fun clearAllMembersForGroup(groupId: String, userId: String)

    @Query("DELETE FROM members")
    suspend fun clear()


}