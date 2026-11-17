package app.paralino.core.db.main.places

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pendingPlaceEventsEntity")
data class PendingPlaceEventsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "placeId") val placeId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "isEnter") val isEnter: Boolean,
    @ColumnInfo(name = "notifyTimeMillis") val notifyTimeMillis: Long
)
