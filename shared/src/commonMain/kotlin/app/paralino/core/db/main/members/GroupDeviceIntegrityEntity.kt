package app.paralino.core.db.main.members

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupDeviceIntegrityEntity")
data class GroupDeviceIntegrityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "integrity") val integrity: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?
)
