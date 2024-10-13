package app.paralino.core.db.main.members

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupDeviceKeysEntity")
data class GroupDeviceKeysEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "memberUserId") val memberUserId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "deviceKeyVersion") val deviceKeyVersion: Long,
    @ColumnInfo(name = "encryptedSenderKey") val encryptedSenderKey: String
)
