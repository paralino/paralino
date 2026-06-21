package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "privacyZoneSharedLocationEntity",
    primaryKeys = ["groupId", "privacyZoneId"],
)
data class PrivacyZoneSharedLocationEntity(
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "privacyZoneId") val privacyZoneId: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "encryptedLocationData") val encryptedLocationData: String,
)
