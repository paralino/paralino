package com.zbapp.locationtrack.data.repos.group

import androidx.room.*
import com.zbapp.locationtrack.data.models.group.UserGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGroupDao {

    @Transaction
    suspend fun replaceGroups(vararg data: UserGroupEntity) {
        clear()
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: UserGroupEntity)

    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<UserGroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroup(id: String): Flow<UserGroupEntity?>

    @Query("SELECT ownerPublicKey FROM groups WHERE id = :groupId")
    suspend fun getGroupPublicKeyOnce(groupId: String): String?

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("DELETE FROM groups")
    suspend fun clear()

}