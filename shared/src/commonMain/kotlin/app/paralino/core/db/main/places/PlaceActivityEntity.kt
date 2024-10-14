package app.paralino.core.db.main.places

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placeActivityEntity")
data class PlaceActivityEntity(
    @PrimaryKey val placeId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "isEnter") val isEnter: Boolean,
    @ColumnInfo(name = "stateChangeTimeMillis") val stateChangeTimeMillis: Long,
    @ColumnInfo(name = "stateChangeTrigger") val stateChangeTrigger: String,
    @ColumnInfo(name = "isScheduledStateEnter") val isScheduledStateEnter: Boolean?,
    @ColumnInfo(name = "isLastNotifyStateEnter") val isLastNotifyStateEnter: Boolean?,
    @ColumnInfo(name = "notifyTrigger") val notifyTrigger: String?,
    @ColumnInfo(name = "scheduleTrigger") val scheduleTrigger: String?,
    @ColumnInfo(name = "notifyTimeMillis") val notifyTimeMillis: Long?
)
