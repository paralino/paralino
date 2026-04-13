package app.paralino.core.db.main.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalHistoryEntity")
data class PersonalHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationId") val locationId: String?,
    @ColumnInfo(name = "historyData") val historyData: String,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
)
