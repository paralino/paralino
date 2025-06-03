package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM unconfirmedLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun getAllUnconfirmedEntriesForGroupOnce(groupId: String): List<UnconfirmedLocationHistoryEntity>

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE id IN (:ids)")
    suspend fun deleteEntries(ids: List<Long>)

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun deleteAllEntriesForGroup(groupId: String)

    @Query("DELETE FROM unconfirmedLocationHistoryEntity WHERE pendingUploadId IN (:ids)")
    suspend fun deleteUnconfirmedUploadEntries(ids: List<String>)

    @Transaction
    suspend fun clearAll() {
        clearAllPending()
        clearAllUnconfirmed()
    }

    @Query("DELETE FROM pendingLocationHistoryEntity")
    suspend fun clearAllPending()

    @Query("DELETE FROM unconfirmedLocationHistoryEntity")
    suspend fun clearAllUnconfirmed()
}