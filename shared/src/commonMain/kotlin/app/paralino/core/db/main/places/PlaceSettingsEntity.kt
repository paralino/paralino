package app.paralino.core.db.main.places

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placeSettingsEntity")
data class PlaceSettingsEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "placeId") val placeId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "deviceId") val deviceId: String?,
    @ColumnInfo(name = "isInPersonalSpace") val isInPersonalSpace: Boolean,
    @ColumnInfo(name = "alertData") val alertData: String
)
