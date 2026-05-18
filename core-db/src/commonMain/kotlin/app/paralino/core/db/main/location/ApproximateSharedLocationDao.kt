package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ApproximateSharedLocationDao {

    @Query("SELECT * FROM approximateSharedLocationEntity WHERE groupId = :groupId LIMIT 1")
    suspend fun getOnce(groupId: String): ApproximateSharedLocationEntity?

    @Upsert
    suspend fun upsert(entity: ApproximateSharedLocationEntity)

    @Query("DELETE FROM approximateSharedLocationEntity WHERE groupId = :groupId")
    suspend fun delete(groupId: String)

    @Query("DELETE FROM approximateSharedLocationEntity")
    suspend fun deleteAll()
}
