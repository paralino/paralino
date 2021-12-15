package com.zbapp.locationtrack.data.repos.invite

import androidx.room.*
import com.zbapp.locationtrack.data.models.invite.InviteCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InviteCodeDao {

    @Transaction
    suspend fun replaceAll(vararg data: InviteCodeEntity) {
        clear()
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: InviteCodeEntity)

    @Query("SELECT * FROM invites")
    fun getAll(): Flow<List<InviteCodeEntity>>

    @Query("SELECT * FROM invites WHERE ownerId = :ownerId")
    fun getForOwner(ownerId: String): Flow<List<InviteCodeEntity>>

    @Query("DELETE FROM invites WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM invites")
    suspend fun clear()

}