package app.paralino.core.db.main.history

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pendingLocationHistoryEntity")
data class PendingLocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "locationId") val locationId: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationHistoryData") val locationHistoryData: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,

    /**
     * This is always null for personal space
    */
    @ColumnInfo(name = "groupHistoryKeyId") val localGroupHistoryKeyId: Long?
)