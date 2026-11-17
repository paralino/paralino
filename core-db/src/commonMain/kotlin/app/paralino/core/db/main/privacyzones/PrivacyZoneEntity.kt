package app.paralino.core.db.main.privacyzones

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "privacyZoneEntity")
data class PrivacyZoneEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "expiresAt") val expiresAt: String,
)
