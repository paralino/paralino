package app.paralino.core.db.main.history

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface UnconfirmedUploadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUnconfirmedEntries(entries: List<UnconfirmedLocationHistoryEntity>)

    @Query("SELECT * FROM unconfirmedLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun getAllUnconfirmedEntriesForGroupOnce(groupId: String): List<UnconfirmedLocationHistoryEntity>

    @Query("SELECT * FROM unconfirmedLocationHistoryEntity")
    suspend fun getAllUnconfirmedEntriesOnce(): List<UnconfirmedLocationHistoryEntity>

    @Query("DELETE FROM unconfirmedLocationHistoryEntity WHERE groupId = :groupId")
    suspend fun deleteAllUnconfirmedEntriesForGroup(groupId: String)

    @Query("DELETE FROM unconfirmedLocationHistoryEntity WHERE pendingUploadId IN (:ids)")
    suspend fun deleteUnconfirmedUploadEntries(ids: List<String>)
}