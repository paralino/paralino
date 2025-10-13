package app.paralino.core.db.main.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.paralino.core.db.main.keys.GroupLocalHistoryKeyEntity

@Dao
interface GroupLocalHistoryKeyDao {
    @Query("SELECT * FROM grouplocalhistorykeyentity WHERE groupId = :groupOrSpaceId ORDER BY id DESC")
    suspend fun getLatestHistoryKey(groupOrSpaceId: String): GroupLocalHistoryKeyEntity?

    @Query("SELECT * FROM grouplocalhistorykeyentity WHERE groupId = :groupId ORDER BY id DESC")
    suspend fun getHistoryKeys(groupId: String): List<GroupLocalHistoryKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalHistoryKey(key: GroupLocalHistoryKeyEntity)
}