package com.zbapp.locationtrack.data.repos.group

import androidx.room.*
import com.zbapp.locationtrack.data.models.group.GroupKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupKeysDao {

    @Transaction
    suspend fun recreateAll(data: List<GroupKeyEntity>) {
        clear()
        createOrUpdate(*data.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: GroupKeyEntity)


    @Query("SELECT * FROM group_keys")
    fun getAll(): Flow<List<GroupKeyEntity>>

    @Query("SELECT * FROM group_keys WHERE groupId = :groupId")
    fun getForGroup(groupId: String): Flow<GroupKeyEntity>

    @Query("SELECT * FROM group_keys WHERE groupId = :groupId")
    fun getForGroupOnce(groupId: String): GroupKeyEntity?

    @Query("SELECT * FROM group_keys WHERE memberId = :memberId")
    fun getForMember(memberId: String): Flow<GroupKeyEntity>


    @Query("DELETE FROM group_keys WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)

    @Query("DELETE FROM group_keys WHERE groupId = :groupId AND memberUserId = :userId")
    suspend fun deleteUserFromGroup(groupId: String, userId: String)

    @Query("DELETE FROM group_keys WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM group_keys")
    suspend fun clear()

}