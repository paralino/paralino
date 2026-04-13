package app.paralino.core.db.main.places

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId

@Entity(tableName = "placeEntity")
data class PlaceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?,
    @ColumnInfo(name = "privatePlace") val privatePlace: Boolean,
    @ColumnInfo(name = "isInPersonalSpace") val isInPersonalSpace: Boolean,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "eventKeys") val eventKeys: String
) {
    @Ignore
    val groupOrSpaceId: GroupOrSpaceId = if (isInPersonalSpace) {
        GroupOrSpaceId.groupPersonalDevicesId.asGroupOrSpaceId()
    } else {
        groupId.asGroupOrSpaceId()
    }
}
