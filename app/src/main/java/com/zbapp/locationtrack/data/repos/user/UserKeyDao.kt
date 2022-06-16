package com.zbapp.locationtrack.data.repos.user

import androidx.room.*
import com.zbapp.locationtrack.data.models.user.UserKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserKeyDao {

    @Transaction
    suspend fun recreateAll(data: UserKeyEntity) {
        clear()
        createOrUpdate(data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: UserKeyEntity)

    @Query("SELECT * FROM user_keys")
    suspend fun getKey(): UserKeyEntity?

    @Query("SELECT * FROM user_keys WHERE version = (SELECT max(version) FROM user_keys)")
    suspend fun getLatest(): UserKeyEntity?


    @Query("DELETE FROM user_keys WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM user_keys")
    suspend fun clear()

}