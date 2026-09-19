package app.paralino.core.db.main.groups

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingGroupJoinWelcomeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingGroupJoinWelcomeEntity)

    @Query("SELECT * FROM pendingGroupJoinWelcomeEntity ORDER BY createdAtMillis ASC")
    fun getAll(): Flow<List<PendingGroupJoinWelcomeEntity>>

    @Query("UPDATE pendingGroupJoinWelcomeEntity SET didShowWelcome = 1 WHERE groupId = :groupId")
    suspend fun markShown(groupId: String)

    @Query("DELETE FROM pendingGroupJoinWelcomeEntity WHERE groupId = :groupId")
    suspend fun delete(groupId: String)

    @Query("DELETE FROM pendingGroupJoinWelcomeEntity")
    suspend fun clearAll()
}
