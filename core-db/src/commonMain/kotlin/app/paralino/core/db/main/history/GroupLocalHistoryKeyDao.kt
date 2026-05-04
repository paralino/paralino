package app.paralino.core.db.main.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.paralino.core.db.main.keys.GroupLocalHistoryKeyEntity

@Dao
interface GroupLocalHistoryKeyDao {

    @Transaction
    suspend fun saveAndActivateHistoryKey(key: GroupLocalHistoryKeyEntity) {
        markKeysAsInactive(groupId = key.groupId)
        saveLocalHistoryKey(key)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKey(key: GroupLocalHistoryKeyEntity)

    @Query("SELECT * FROM groupLocalHistoryKeyEntity WHERE groupId = :groupId AND isActive = 1 ORDER BY id DESC")
    suspend fun getActiveHistoryKey(groupId: String): GroupLocalHistoryKeyEntity?

    @Query("SELECT * FROM groupLocalHistoryKeyEntity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getAllHistoryKeys(groupId: String): List<GroupLocalHistoryKeyEntity>

    @Query("UPDATE OR IGNORE groupLocalHistoryKeyEntity SET isActive = 0 WHERE groupId = :groupId")
    suspend fun markKeysAsInactive(groupId: String)

    @Transaction
    suspend fun deleteAllInactiveKeys(groupId: String) {
        val usedKeyIds = getUsedPendingKeyIdsForGroupOnce(groupOrSpaceId = groupId)
        deleteUnusedInactiveKeys(groupId = groupId, usedKeyIds = usedKeyIds)
    }

    @Query("DELETE FROM groupLocalHistoryKeyEntity WHERE groupId = :groupId AND isActive = 0 AND id NOT IN (:usedKeyIds)")
    suspend fun deleteUnusedInactiveKeys(groupId: String, usedKeyIds: List<Long>)

    @Query("SELECT groupHistoryKeyId FROM pendingLocationHistoryEntity WHERE groupId = :groupOrSpaceId")
    suspend fun getUsedPendingKeyIdsForGroupOnce(groupOrSpaceId: String): List<Long>
}