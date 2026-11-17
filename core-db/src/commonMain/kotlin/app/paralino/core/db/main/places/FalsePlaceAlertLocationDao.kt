package app.paralino.core.db.main.places

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface FalsePlaceAlertLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FalsePlaceAlertLocationEntity)

    @Query("SELECT * FROM falsePlaceAlertLocationEntity")
    suspend fun getAll(): List<FalsePlaceAlertLocationEntity>

    @Query("DELETE FROM falsePlaceAlertLocationEntity WHERE placeId = :placeId")
    suspend fun delete(placeId: String)
}
