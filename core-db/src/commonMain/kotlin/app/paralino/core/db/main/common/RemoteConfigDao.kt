package app.paralino.core.db.main.common

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteConfigDao {

    @Transaction
    suspend fun recreateConfig(config: List<RemoteConfigEntity>) {
        clearAll()
        createConfig(config)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createConfig(config: List<RemoteConfigEntity>)

    @Query("SELECT * FROM configNodeEntity")
    suspend fun getAllOnce(): List<RemoteConfigEntity>

    @Query("SELECT * FROM configNodeEntity")
    fun getAll(): Flow<List<RemoteConfigEntity>>

    @Query("DELETE FROM configNodeEntity")
    suspend fun clearAll()
}
