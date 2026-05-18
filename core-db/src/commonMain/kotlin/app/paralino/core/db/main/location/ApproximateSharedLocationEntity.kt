package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "approximateSharedLocationEntity")
data class ApproximateSharedLocationEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "encryptedLocationData") val encryptedLocationData: String,
)
