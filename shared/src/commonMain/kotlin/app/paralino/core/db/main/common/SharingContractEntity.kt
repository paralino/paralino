package app.paralino.core.db.main.common

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sharingContractEntity")
data class SharingContractEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "endTimeUTC") val endTimeUTC: String?,
    @ColumnInfo(name = "shareBattery") val shareBattery: Boolean,
    @ColumnInfo(name = "shareSpeed") val shareSpeed: Boolean,
    @ColumnInfo(name = "shareElevation") val shareElevation: Boolean,
    @ColumnInfo(name = "approximateLocationInMeters") val approximateLocationInMeters: Int?,
    @ColumnInfo(name = "shareNetwork") val shareNetwork: Boolean,
    @ColumnInfo(name = "shareDeviceStatus") val shareDeviceStatus: Boolean = true
)
