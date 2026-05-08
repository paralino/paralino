package app.paralino.core.db.main.common

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sharingContractEntity")
data class SharingContractEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "endTimeUTC") val endTimeUTC: String?,
    @ColumnInfo(name = "endTimeUTCOverride") val endTimeUTCOverride: String? = null,
    @ColumnInfo(name = "shareBattery") val shareBattery: Boolean,
    @ColumnInfo(name = "shareSpeed") val shareSpeed: Boolean,
    @ColumnInfo(name = "shareElevation") val shareElevation: Boolean,
    @ColumnInfo(name = "approximateLocationInMeters") val approximateLocationInMeters: Int?,
    @ColumnInfo(name = "shareNetwork") val shareNetwork: Boolean,
    @ColumnInfo(name = "shareDeviceStatus") val shareDeviceStatus: Boolean = true,
    @ColumnInfo(name = "saveToHistory") val saveToHistory: Boolean = false,
    @ColumnInfo(name = "isNotifyOthersOn") val isNotifyOthersOn: Boolean? = null,
    @ColumnInfo(name = "sharingDurationPreset") val sharingDurationPreset: Long? = null,
    @ColumnInfo(name = "isEnabled") val isEnabled: Boolean = true
)
