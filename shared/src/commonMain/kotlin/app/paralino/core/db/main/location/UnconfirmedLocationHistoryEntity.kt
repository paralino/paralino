package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unconfirmedLocationHistoryEntity")
data class UnconfirmedLocationHistoryEntity(
    @PrimaryKey val pendingUploadId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationHistoryData") val locationHistoryData: String,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
)