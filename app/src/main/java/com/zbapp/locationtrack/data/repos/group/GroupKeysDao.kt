package com.zbapp.locationtrack.data.repos.group

import androidx.room.*
import com.zbapp.locationtrack.data.models.group.GroupKeyEntity
import com.zbapp.locationtrack.data.models.group.GroupMemberKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupKeysDao {

    @Transaction
    suspend fun recreateAllKeys(groupKeys: List<GroupKeyEntity>, memberKeys: List<GroupMemberKeyEntity>) {
        clearGroupKeys()
        clearMemberKeys()
        createOrUpdate(*groupKeys.toTypedArray())
        createOrUpdate(*memberKeys.toTypedArray())
    }

    @Transaction
    suspend fun recreateAllGroupKeys(data: List<GroupKeyEntity>) {
        clearGroupKeys()
        createOrUpdate(*data.toTypedArray())
    }

    @Transaction
    suspend fun recreateAllGroupKeysForGroup(groupId: String, groupKeys: List<GroupKeyEntity>, memberKeys: List<GroupMemberKeyEntity>) {
        deleteGroupKeysForGroup(groupId)
        deleteMemberKeysForGroup(groupId)
        createOrUpdate(*groupKeys.toTypedArray())
        createOrUpdate(*memberKeys.toTypedArray())
    }

    @Transaction
    suspend fun recreateGroupKeysForGroup(groupId: String, data: List<GroupKeyEntity>) {
        deleteGroupKeysForGroup(groupId)
        createOrUpdate(*data.toTypedArray())
    }

    @Transaction
    suspend fun recreateAllMemberKeys(data: List<GroupMemberKeyEntity>) {
        clearMemberKeys()
        createOrUpdate(*data.toTypedArray())
    }

    @Transaction
    suspend fun recreateMemberKeysForGroup(groupId: String, data: List<GroupMemberKeyEntity>) {
        deleteMemberKeysForGroup(groupId)
        createOrUpdate(*data.toTypedArray())
    }



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupMemberKeyEntity)



    @Query("SELECT * FROM group_keys")
    fun getAllGroupKeys(): Flow<List<GroupKeyEntity>>

    @Query("SELECT * FROM group_member_keys")
    fun getAllMemberKeys(): Flow<List<GroupMemberKeyEntity>>



    @Query("DELETE FROM group_keys WHERE groupId = :groupId")
    suspend fun deleteGroupKeysForGroup(groupId: String)

    @Query("DELETE FROM group_member_keys WHERE groupId = :groupId")
    suspend fun deleteMemberKeysForGroup(groupId: String)

    @Query("DELETE FROM group_member_keys WHERE groupId = :groupId AND memberUserId = :userId")
    suspend fun deleteUserFromGroup(groupId: String, userId: String)

    @Transaction
    suspend fun deleteAllKeysForGroup(groupId: String) {
        deleteGroupKeysForGroup(groupId)
        deleteMemberKeysForGroup(groupId)
    }


    @Query("DELETE FROM group_member_keys")
    suspend fun clearMemberKeys()

    @Query("DELETE FROM group_keys")
    suspend fun clearGroupKeys()

    @Transaction
    suspend fun clear() {
        clearGroupKeys()
        clearMemberKeys()
    }

}