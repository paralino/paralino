package app.paralino.core.db.main.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPendingEntries(entries: List<PendingLocationHistoryEntity>)


    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE id = :id")
    suspend fun getEntryOnce(id: Long): PendingLocationHistoryEntity?

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    fun getAllEntries(): Flow<List<PendingLocationHistoryEntity>>

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    suspend fun getAllEntriesOnce(): List<PendingLocationHistoryEntity>

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE groupId = :groupOrSpaceId")
    suspend fun getAllEntriesForGroupOnce(groupOrSpaceId: String): List<PendingLocationHistoryEntity>

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE groupId = :groupOrSpaceId ORDER BY id DESC")
    suspend fun getLatestPendingEntryOnce(groupOrSpaceId: String): PendingLocationHistoryEntity?

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE id IN (:ids)")
    suspend fun deletePendingEntries(ids: List<Long>)

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE groupId = :groupOrSpaceId")
    suspend fun deleteAllPendingEntriesForGroup(groupOrSpaceId: String)
}