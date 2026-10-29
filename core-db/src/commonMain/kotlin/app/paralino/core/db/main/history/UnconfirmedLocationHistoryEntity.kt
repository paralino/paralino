package app.paralino.core.db.main.history

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "unconfirmedLocationHistoryEntity")
data class UnconfirmedLocationHistoryEntity(
    @PrimaryKey val pendingUploadId: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationHistoryData") val locationHistoryData: String,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
)