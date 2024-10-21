package app.paralino.core.db.main.blocklist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {

    @Transaction
    suspend fun recreateBlocklist(blocklist: List<BlocklistEntity>) {
        clearAll()
        createBlocklist(blocklist)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createBlocklist(blocklist: List<BlocklistEntity>)

    @Query("SELECT * FROM blocklistEntity")
    fun getBlocklist(): Flow<List<BlocklistEntity>>

    @Query("DELETE FROM blocklistEntity WHERE id = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM blocklistEntity")
    suspend fun clearAll()
}
