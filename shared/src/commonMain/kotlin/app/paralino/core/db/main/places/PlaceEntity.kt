package app.paralino.core.db.main.places

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placeEntity")
data class PlaceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "privatePlace") val privatePlace: Boolean,
    @ColumnInfo(name = "data") val data: String
)
