package app.paralino.core.db.main.location

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "approximateSharedLocationEntity")
data class ApproximateSharedLocationEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "encryptedLocationData") val encryptedLocationData: String,
)
