package app.paralino.core.db.main.history

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import app.paralino.core.db.main.keys.GroupLocalHistoryKeyEntity

@Dao
interface UnsyncedGroupHistoryKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKeys(keys: List<UnsyncedGroupHistoryKeyEntity>)

    @Query("SELECT * FROM unsyncedGroupHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getHistoryKeysForGroup(groupId: String): List<UnsyncedGroupHistoryKeyEntity>

    @Query("UPDATE unsyncedGroupHistoryKeyEntity SET isSyncedOnce = 1 WHERE groupId = :groupId AND memberId = :memberId AND historyKeyVersion = :historyKeyVersion")
    suspend fun markKeysAsSyncedOnce(groupId: String, memberId: String, historyKeyVersion: String)

    @Query("DELETE FROM unsyncedGroupHistoryKeyEntity WHERE groupId = :groupId AND memberId = :memberId AND historyKeyVersion = :historyKeyVersion")
    suspend fun deleteKeys(groupId: String, memberId: String, historyKeyVersion: String)
}