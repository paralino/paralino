package app.paralino.core.db.main.user

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
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

    @Query("SELECT * FROM userDeviceEntity WHERE id = :id")
    suspend fun getUserDeviceOnce(id: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity WHERE deviceId = :deviceId")
    suspend fun getUserDeviceByDeviceIdOnce(deviceId: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity WHERE sessionId = :sessionId")
    suspend fun getUserDeviceBySessionIdOnce(sessionId: String): UserDeviceEntity?

    @Query("SELECT * FROM userDeviceEntity")
    fun getAllUserDevices(): Flow<List<UserDeviceEntity>>

    @Query("SELECT * FROM userDeviceEntity")
    suspend fun getAllUserDevicesOnce(): List<UserDeviceEntity>

    @Update(entity = UserDeviceEntity::class)
    suspend fun updateUserDeviceLocationData(dataUpdate: UserDeviceEntityLocationDataUpdate)

    @Query("""
        UPDATE userDeviceEntity SET
            locationData = CASE WHEN :locationData IS NULL THEN locationData ELSE :locationData END,
            status = CASE WHEN :status IS NULL THEN status ELSE :status END,
            locationHistory = CASE WHEN :locationHistory IS NULL THEN locationHistory ELSE :locationHistory END
        WHERE id = :id
    """)
    suspend fun updateUserDeviceLocationDataPartial(
        id: String,
        locationData: String?,
        status: String?,
        locationHistory: String?
    )

    @Update(entity = UserDeviceEntity::class)
    suspend fun updateUserDeviceData(dataUpdate: UserDeviceEntityDeviceUpdate)

    @Query("DELETE FROM userDeviceEntity WHERE id = :deviceId")
    suspend fun deleteUserDevice(deviceId: String)

    @Query("DELETE FROM userDeviceEntity WHERE sessionId = :sessionId")
    suspend fun deleteUserDeviceBySessionId(sessionId: String)

    @Query("DELETE FROM userDeviceEntity")
    suspend fun clearAllUserDevices()

    @Query("UPDATE OR IGNORE userDeviceEntity SET locationData = NULL, locationHistory = NULL WHERE id = :locationId")
    suspend fun clearLocationDataForDevice(locationId: String)

    @Query("UPDATE OR IGNORE userDeviceEntity SET status = NULL WHERE id = :locationId")
    suspend fun clearStatusForDevice(locationId: String)
}