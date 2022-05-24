package com.zbapp.core.presentation.share

import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.core.utils.SharingDurationPreset

data class SharingStartScreenState constructor(
    val groupState: Payload<GroupItemData?> = Payload.Loading,
    val sharingTimeMillis: Long = SharingDurationPreset.hour,
    val sharingPresetsMillis: List<Long> = SharingDurationPreset.presets,
    val approximateLocationMeters: Int = 200,
    val approximateLocationPresetsMeters: List<Int> = listOf(100, 200, 300, 500, 1000, 2000, 5000, 10000),
    val shareApproximateLocation: Boolean = false,
    val shareBattery: Boolean = true,
    val shareSpeed: Boolean = false,
    val shareElevation: Boolean = false,
    val errors: List<ConditionErrorType> = emptyList(),
    val loadingAction: Boolean = false
) {
    val sharingTimeIndex: Int get() {
        return sharingPresetsMillis.indexOfFirst { it == sharingTimeMillis }.takeIf { it >= 0 }
            ?: sharingPresetsMillis.indexOfFirst { sharingTimeMillis < it }.takeIf { it >= 0 }
            ?: sharingPresetsMillis.lastIndex
    }

    val approximateLocationIndex: Int get() {
        return approximateLocationPresetsMeters.indexOfFirst { it == approximateLocationMeters }.takeIf { it >= 0 }
            ?: approximateLocationPresetsMeters.indexOfFirst { approximateLocationMeters < it }.takeIf { it >= 0 }
            ?: approximateLocationPresetsMeters.lastIndex
    }

    val isLongTerm: Boolean get() = LocationSharingContract.isLongTerm(sharingTimeMillis)
}

sealed class SharingStartScreenEvent() {
    data class Error(val error: Throwable): SharingStartScreenEvent()
    object Started: SharingStartScreenEvent()
    object SelectGroupInfo: SharingStartScreenEvent()
    object LocationStatusInsufficient: SharingStartScreenEvent()
    object MissingBackgroundPermission: SharingStartScreenEvent()
}
