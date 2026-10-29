package app.paralino.core.db.main.groups

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "batteryAlertEvent")
data class BatteryAlertEventEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "notifyTimeMillis") val notifyTimeMillis: Long
)