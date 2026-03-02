package app.paralino.core.db.main.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userDeviceEntity")
data class UserDeviceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "pushTargetId") val pushTargetId: String?,
    @ColumnInfo(name = "sessionId") val sessionId: String?,
    @ColumnInfo(name = "deviceType") val deviceType: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "deviceId") val deviceId: String,
    @ColumnInfo(name = "locationPushToken") val locationPushToken: String?,
    @ColumnInfo(name = "batteryAlertSettings") val batteryAlertSettings: String?,
    @ColumnInfo(name = "encryptedSenderKey") val encryptedSenderKey: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?,
    @ColumnInfo(name = "locationId") val locationId: String?,
    @ColumnInfo(name = "deviceData") val deviceData: String?,
    @ColumnInfo(name = "locationData") val locationData: String?,
    @ColumnInfo(name = "status") val status: String?,
    @ColumnInfo(name = "locationHistory") val locationHistory: String?,
    @ColumnInfo(name = "unifiedPushConfig") val unifiedPushConfig: String?,
    @ColumnInfo(name = "pushProvider") val pushProvider: String?
) {
    val hasValidLocation: Boolean get() = !locationId.isNullOrBlank() && !encryptedSenderKey.isNullOrBlank() && userKeyVersion != null
}

@Entity
data class UserDeviceEntityDeviceUpdate(
    @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "pushTargetId") val pushTargetId: String?,
    @ColumnInfo(name = "sessionId") val sessionId: String?,
    @ColumnInfo(name = "deviceType") val deviceType: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "deviceId") val deviceId: String,
    @ColumnInfo(name = "deviceData") val deviceData: String?,
    @ColumnInfo(name = "locationPushToken") val locationPushToken: String?,
    @ColumnInfo(name = "encryptedSenderKey") val encryptedSenderKey: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?,
    @ColumnInfo(name = "batteryAlertSettings") val batteryAlertSettings: String?,
    @ColumnInfo(name = "unifiedPushConfig") val unifiedPushConfig: String?,
    @ColumnInfo(name = "pushProvider") val pushProvider: String?
)

@Entity
data class UserDeviceEntityLocationDataUpdate(
    @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "locationData") val locationData: String?,
    @ColumnInfo(name = "status") val status: String?,
    @ColumnInfo(name = "locationHistory") val locationHistory: String?,
)