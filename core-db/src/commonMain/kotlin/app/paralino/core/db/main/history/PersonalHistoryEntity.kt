package app.paralino.core.db.main.history

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "personalHistoryEntity")
data class PersonalHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationId") val locationId: String?,
    @ColumnInfo(name = "historyData") val historyData: String,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
)
