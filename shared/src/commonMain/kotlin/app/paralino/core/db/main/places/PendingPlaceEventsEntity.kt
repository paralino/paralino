package app.paralino.core.db.main.places

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pendingPlaceEventsEntity")
data class PendingPlaceEventsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "placeId") val placeId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "isEnter") val isEnter: Boolean,
    @ColumnInfo(name = "notifyTimeMillis") val notifyTimeMillis: Long
)
