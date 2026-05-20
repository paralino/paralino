package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PrivacyZoneSharedLocationDao {

    @Query(
        """
        SELECT * FROM privacyZoneSharedLocationEntity
        WHERE groupId = :groupId AND privacyZoneId = :privacyZoneId
        LIMIT 1
        """
    )
    suspend fun getOnce(groupId: String, privacyZoneId: String): PrivacyZoneSharedLocationEntity?

    @Upsert
    suspend fun upsert(entity: PrivacyZoneSharedLocationEntity)

    @Query(
        "DELETE FROM privacyZoneSharedLocationEntity WHERE groupId = :groupId AND privacyZoneId = :privacyZoneId"
    )
    suspend fun delete(groupId: String, privacyZoneId: String)

    @Query("DELETE FROM privacyZoneSharedLocationEntity WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)

    @Query("DELETE FROM privacyZoneSharedLocationEntity")
    suspend fun deleteAll()
}
