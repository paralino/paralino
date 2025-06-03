package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity

@Dao
interface LocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalKeys(entries: List<PersonalHistoryKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalHistory(entries: List<PersonalHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupKeys(entries: List<GroupHistoryKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupHistory(entries: List<GroupHistoryEntity>)



    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getLatestHistoryKey(groupId: String): LocalLocationHistoryKeyEntity?

    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getHistoryKeys(groupId: String): List<LocalLocationHistoryKeyEntity>

    @Query("SELECT * FROM localLocationHistoryKeyEntity WHERE groupId = :groupId AND isSynced = 1 ORDER BY id DESC")
    suspend fun getUnsyncedHistoryKeys(groupId: String): List<LocalLocationHistoryKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKey(key: LocalLocationHistoryKeyEntity)

    @Query("UPDATE localLocationHistoryKeyEntity SET isSynced = :synced WHERE groupId = :groupId AND historyKeyVersion = :historyKeyVersion")
    suspend fun updateLocalHistoryKey(groupId: String, historyKeyVersion: String, synced: Boolean)
}