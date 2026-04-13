package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "localDeviceKeysEntity")
data class LocalGroupDeviceKeysEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "deviceKeyVersion") val deviceKeyVersion: Long,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "encryptedSenderKey") val encryptedSenderKey: String
)
