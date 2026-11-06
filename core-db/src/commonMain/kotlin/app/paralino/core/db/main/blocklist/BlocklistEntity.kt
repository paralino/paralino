package app.paralino.core.db.main.blocklist

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "blocklistEntity")
data class BlocklistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "resourceType") val resourceType: String,
    @ColumnInfo(name = "resourceId") val resourceId: String,
    @ColumnInfo(name = "data") val data: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?
)
