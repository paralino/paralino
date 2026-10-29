package app.paralino.core.db.main.places

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
