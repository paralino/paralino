package app.paralino.core.db.main.keys

import androidx.room3.*
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
    fun getAll(): Flow<List<UserKeysEntity>>

    @Query("SELECT * FROM userKeysEntity WHERE isActive = 1 LIMIT 1")
    fun getActive(): Flow<UserKeysEntity?>

    @Query("SELECT * FROM userKeysEntity WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveOnce(): UserKeysEntity?

    @Query("DELETE FROM userKeysEntity")
    suspend fun clearAll()
}
