package app.paralino.core.db.main.places

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface WifiAnchorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WifiAnchorEntity)

    @Query("SELECT * FROM wifiAnchorEntity")
    suspend fun getAll(): List<WifiAnchorEntity>

    @Query("DELETE FROM wifiAnchorEntity WHERE id = :id")
    suspend fun delete(id: String)
}
