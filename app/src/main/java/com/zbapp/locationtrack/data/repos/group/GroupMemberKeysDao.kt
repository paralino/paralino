package com.zbapp.locationtrack.data.repos.group

import androidx.room.*
import com.zbapp.locationtrack.data.models.group.GroupMemberKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberKeysDao {

    @Transaction
    suspend fun recreateAll(data: List<GroupMemberKeyEntity>) {
        clear()
        createOrUpdate(*data.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupMemberKeyEntity)


    @Query("SELECT * FROM group_member_keys")
    fun getAll(): Flow<List<GroupMemberKeyEntity>>

    @Query("SELECT * FROM group_member_keys WHERE groupId = :groupId")
    fun getForGroup(groupId: String): Flow<List<GroupMemberKeyEntity>>

    @Query("SELECT * FROM group_member_keys WHERE groupId = :memberId")
    fun getForMember(memberId: String): Flow<GroupMemberKeyEntity>


    @Query("DELETE FROM group_member_keys WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM group_member_keys WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)

    @Query("DELETE FROM group_member_keys WHERE groupId = :groupId AND memberUserId = :userId")
    suspend fun deleteUserFromGroup(groupId: String, userId: String)

    @Query("DELETE FROM group_member_keys")
    suspend fun clear()

}