package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupHistoryEntity")
data class GroupHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "createdAt") val createdAt: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "locationId") val locationId: String?,
    @ColumnInfo(name = "deviceId") val deviceId: String?,
    @ColumnInfo(name = "historyData") val historyData: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String,
)