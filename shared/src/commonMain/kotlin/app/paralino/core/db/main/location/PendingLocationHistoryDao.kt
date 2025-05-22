package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createEntries(entries: List<PendingLocationHistoryEntity>)

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE id = :id")
    suspend fun getEntryOnce(id: Long): PendingLocationHistoryEntity?

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    fun getAllEntries(): Flow<List<PendingLocationHistoryEntity>>

    @Query("SELECT * FROM pendingLocationHistoryEntity")
    suspend fun getAllEntriesOnce(): List<PendingLocationHistoryEntity>

    @Query("SELECT * FROM pendingLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun getAllEntriesForGroupOnce(groupId: String): List<PendingLocationHistoryEntity>

    @Query("DELETE FROM pendingLocationHistoryEntity WHERE id IN (:ids)")
    suspend fun deleteEntries(ids: List<Long>)

    @Query("DELETE FROM pendingLocationHistoryEntity")
    suspend fun clearAll()
}