package app.paralino.core.db.main.places

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "wifiAnchorEntity")
data class WifiAnchorEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "anchorData") val anchorData: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "updatedAtMillis") val updatedAtMillis: Long,
)
