package app.paralino.core.db.main.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memberLocationEntity")
data class MemberLocationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "memberId") val memberId: String,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "deviceId") val deviceId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "deviceKeyVersion") val deviceKeyVersion: Long,
    @ColumnInfo(name = "deviceData") val deviceData: String?,
    @ColumnInfo(name = "locationData") val locationData: String?,
    @ColumnInfo(name = "locationHistory") val locationHistory: String? = null,
    @ColumnInfo(name = "status") val status: String?
)

@Entity
data class MemberLocationEntityDataUpdate(
    @ColumnInfo(name = "id") var id: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "deviceKeyVersion") val deviceKeyVersion: Long,
    @ColumnInfo(name = "deviceId") val deviceId: String,
    @ColumnInfo(name = "deviceData") val deviceData: String?,
    @ColumnInfo(name = "locationData") val locationData: String?,
    @ColumnInfo(name = "locationHistory") val locationHistory: String?,
    @ColumnInfo(name = "status") val status: String?
)