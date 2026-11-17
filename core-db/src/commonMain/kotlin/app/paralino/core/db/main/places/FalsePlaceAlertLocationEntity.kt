package app.paralino.core.db.main.places

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "falsePlaceAlertLocationEntity")
data class FalsePlaceAlertLocationEntity(
    @PrimaryKey val placeId: String,
    @ColumnInfo(name = "locationsData") val locationsData: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "updatedAtMillis") val updatedAtMillis: Long,
)
