package app.paralino.core.db.main.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batteryAlertEvent")
data class BatteryAlertEventEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "notifyTimeMillis") val notifyTimeMillis: Long
)