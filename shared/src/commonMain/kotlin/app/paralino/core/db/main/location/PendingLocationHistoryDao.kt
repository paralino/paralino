package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPendingEntries(entries: List<PendingLocationHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUnconfirmedEntries(entries: List<UnconfirmedLocationHistoryEntity>)



    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE id = :id")
    suspend fun getEntryOnce(id: Long): PendingLocationHistoryEntity?

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    fun getAllEntries(): Flow<List<PendingLocationHistoryEntity>>

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    suspend fun getAllEntriesOnce(): List<PendingLocationHistoryEntity>

    @Query("SELECT * FROM unconfirmedLocationHistoryEntity")
    suspend fun getAllUnconfirmedEntriesOnce(): List<UnconfirmedLocationHistoryEntity>

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun getAllEntriesForGroupOnce(groupId: String): List<PendingLocationHistoryEntity>

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getLatestPendingEntryOnce(groupId: String): PendingLocationHistoryEntity?

    @Query("SELECT * FROM unconfirmedLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun getAllUnconfirmedEntriesForGroupOnce(groupId: String): List<UnconfirmedLocationHistoryEntity>




    @Query("DELETE FROM pendingLocationHistoryEntity WHERE id IN (:ids)")
    suspend fun deletePendingEntries(ids: List<Long>)

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun deleteAllPendingEntriesForGroup(groupId: String)

    @Query("DELETE FROM unconfirmedLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun deleteAllUnconfirmedEntriesForGroup(groupId: String)

    @Query("DELETE FROM unconfirmedLocationHistoryEntity WHERE pendingUploadId IN (:ids)")
    suspend fun deleteUnconfirmedUploadEntries(ids: List<String>)



    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getLatestHistoryKey(groupId: String): LocalLocationHistoryKeyEntity?

    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getHistoryKeys(groupId: String): List<LocalLocationHistoryKeyEntity>

    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId AND isSynced = 0 ORDER BY id DESC")
    suspend fun getUnsyncedHistoryKeys(groupId: String): List<LocalLocationHistoryKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKey(key: LocalLocationHistoryKeyEntity)

    @Query("UPDATE localLocationHistoryKeyEntity SET isSynced = :synced WHERE groupId = :groupId AND historyKeyVersion = :historyKeyVersion")
    suspend fun updateLocalHistoryKey(groupId: String, historyKeyVersion: String, synced: Boolean)
}