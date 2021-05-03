package com.zbapp.locationtrack.data.repos.group

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zbapp.locationtrack.data.models.group.UserGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: UserGroupEntity)

    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<UserGroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroup(id: String): Flow<UserGroupEntity>

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("DELETE FROM groups")
    suspend fun clear()

}