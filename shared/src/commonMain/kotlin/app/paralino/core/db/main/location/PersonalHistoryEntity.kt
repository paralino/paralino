package app.paralino.core.db.main.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.paralino.core.models.raw.LocationHistoryReadEntry

@Entity(tableName = "personalHistoryEntity")
data class PersonalHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "dateIdentifier") val dateIdentifier: String,
    @ColumnInfo(name = "locationId") val locationId: String?,
    @ColumnInfo(name = "deviceId") val deviceId: String?,
    @ColumnInfo(name = "historyData") val historyData: String,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
) {
    fun toReadModel(): LocationHistoryReadEntry {
        return LocationHistoryReadEntry(
            id = this.id,
            dateKey = this.dateIdentifier,
            deviceId = this.deviceId,
            locationId = this.locationId,
            keyVersion = this.historyKeyVersion,
            historyData = this.historyData,
        )
    }
}
