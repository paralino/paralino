package app.paralino.core.db.main.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUserDevice(device: UserDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUserDevices(devices: List<UserDeviceEntity>)

    @Transaction
    suspend fun recreateUserDevices(devices: List<UserDeviceEntity>) {
        clearAllUserDevices()
        createUserDevices(devices)
    }

    @Query("SELECT * FROM userDeviceEntity WHERE id = :deviceId")
    fun getUserDevice(deviceId: String): Flow<UserDeviceEntity?>

    @Query("SELECT * FROM userDeviceEntity WHERE id = :deviceId")
    suspend fun getUserDeviceOnce(deviceId: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity WHERE deviceId = :deviceId")
    suspend fun getUserDeviceByDeviceIdOnce(deviceId: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity WHERE sessionId = :sessionId")
    suspend fun getUserDeviceBySessionIdOnce(sessionId: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity")
    fun getAllUserDevices(): Flow<List<UserDeviceEntity>>

    @Query("SELECT * FROM userDeviceEntity")
    suspend fun getAllUserDevicesOnce(): List<UserDeviceEntity>

    @Update(entity = UserDeviceEntity::class)
    suspend fun updateUserDeviceLocationData(dataUpdate: UserDeviceLocationDataUpdate)

    @Query("DELETE FROM userDeviceEntity WHERE id = :deviceId")
    suspend fun deleteUserDevice(deviceId: String)

    @Query("DELETE FROM userDeviceEntity WHERE sessionId = :sessionId")
    suspend fun deleteUserDeviceBySessionId(sessionId: String)

    @Query("DELETE FROM userDeviceEntity")
    suspend fun clearAllUserDevices()

    @Query("UPDATE OR IGNORE userDeviceEntity SET locationData = NULL WHERE id = :deviceId")
    suspend fun clearLocationDataForDevice(deviceId: String)

    @Query("UPDATE OR IGNORE userDeviceEntity SET status = NULL WHERE id = :deviceId")
    suspend fun clearStatusForDevice(deviceId: String)
}

data class UserDeviceLocationDataUpdate(
    val id: String,
    val deviceData: String?,
    val locationData: String?,
    val status: String?,
    val locationHistory: String?
) 