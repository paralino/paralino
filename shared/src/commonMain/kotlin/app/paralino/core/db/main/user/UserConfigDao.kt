package app.paralino.core.db.main.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConfigDao {

    @Transaction
    suspend fun recreateConfig(config: List<UserConfigEntity>) {
        clearAll()
        createConfig(config)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createConfig(config: List<UserConfigEntity>)

    @Query("SELECT * FROM userConfigEntity")
    fun getAll(): Flow<List<UserConfigEntity>>

    @Query("DELETE FROM userConfigEntity")
    suspend fun clearAll()
}
