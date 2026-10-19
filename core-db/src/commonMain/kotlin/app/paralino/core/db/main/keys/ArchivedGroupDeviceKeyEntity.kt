package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "archivedGroupDeviceKeyEntity",
    // there is no actual id here since the real key no longer exists
    primaryKeys = ["groupId", "ownerUserId", "locationId"]
)
data class ArchivedGroupDeviceKeyEntity(
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "deviceKeyVersion") val deviceKeyVersion: Long,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "encryptedSenderKey") val encryptedSenderKey: String,
    @ColumnInfo(name = "archivedAtMillis") val archivedAtMillis: Long
)
