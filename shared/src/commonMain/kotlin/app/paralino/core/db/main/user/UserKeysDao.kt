package app.paralino.core.db.main.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserKeysDao {

    @Transaction
    suspend fun recreateUserKeys(key: UserKeysEntity) {
        clearAll()
        createUserKey(key)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUserKey(key: UserKeysEntity)

    @Query("SELECT * FROM userKeysEntity")
    suspend fun getAll(): List<UserKeysEntity>

    @Query("SELECT * FROM userKeysEntity WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<UserKeysEntity?>

    @Query("DELETE FROM userKeysEntity")
    suspend fun clearAll()
}
