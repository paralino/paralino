package app.paralino.core.db.logs.accesslog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccessLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessLog(accessLog: AccessLogEntity)

    @Query("SELECT * FROM access_logs WHERE type = :type AND access_id = :accessId")
    suspend fun getAccessLogs(type: String, accessId: String): List<AccessLogEntity>

    @Query("SELECT * FROM access_logs")
    suspend fun getAllAccessLogs(): List<AccessLogEntity>

    @Query("DELETE FROM access_logs WHERE access_id = :accessId")
    suspend fun deleteAccessLogForAccessId(accessId: String)

    @Query("DELETE FROM access_logs WHERE type = :type AND access_id = :accessId")
    suspend fun deleteAccessLog(type: String, accessId: String)

    @Query("DELETE FROM access_logs")
    suspend fun clearAll()
}