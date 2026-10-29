package app.paralino.core.db.main.history

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import app.paralino.core.db.main.keys.PersonalLocalHistoryKeyEntity

@Dao
interface PersonalLocalHistoryKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKey(key: PersonalLocalHistoryKeyEntity)

    @Query("SELECT * FROM personalLocalHistoryKeyEntity ORDER BY id DESC")
    suspend fun getLatestHistoryKey(): PersonalLocalHistoryKeyEntity?

    @Query("SELECT * FROM personalLocalHistoryKeyEntity WHERE isSynced = 0 ORDER BY id DESC")
    suspend fun getAllUnsyncedHistoryKeys(): List<PersonalLocalHistoryKeyEntity>

    @Query("SELECT historyKeyVersion FROM personalLocalHistoryKeyEntity ORDER BY id DESC")
    suspend fun getAllHistoryKeyVersions(): List<String>

    @Query("UPDATE personalLocalHistoryKeyEntity SET isSynced = :isSynced WHERE historyKeyVersion = :historyKeyVersion")
    suspend fun updateLocalHistoryKey(historyKeyVersion: String, isSynced: Boolean)

    @Query("DELETE FROM personalLocalHistoryKeyEntity WHERE isSynced = 1 AND historyKeyVersion != :historyKeyToKeep")
    suspend fun deleteAllSyncedKeysBut(historyKeyToKeep: String)

    @Transaction
    suspend fun markKeysAsSynced(syncedKeysVersions: List<String>) {
        syncedKeysVersions.forEach {
            updateLocalHistoryKey(
                historyKeyVersion = it,
                isSynced = true
            )
        }

        clearUnusedSyncedKeys()
    }

    @Transaction
    suspend fun clearUnusedSyncedKeys() {
        getLatestHistoryKey()?.let {
            deleteAllSyncedKeysBut(historyKeyToKeep = it.historyKeyVersion)
        }
    }
}