package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pendingLocationHistoryEntity")
data class PendingLocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "locationHistoryData") val locationHistoryData: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "historyKeyId") val localHistoryKeyId: Long
)