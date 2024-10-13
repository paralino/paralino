package app.paralino.core.db.main.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocklistEntity")
data class BlocklistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "resourceType") val resourceType: String,
    @ColumnInfo(name = "resourceId") val resourceId: String,
    @ColumnInfo(name = "data") val data: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?
)
