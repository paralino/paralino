package app.paralino.core.presentation.share

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupDetailScreenEvent
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset

data class SharingStartScreenState constructor(
    val groupState: Payload<GroupItemState?> = Payload.Loading,
    val sharingTimeMillis: Long = SharingDurationPreset.infinite,
    val sharingPresetsMillis: List<Long> = SharingDurationPreset.presets,
    val approximateLocationMeters: Int = 200,
    val approximateLocationPresetsMeters: List<Int> = listOf(100, 200, 300, 500, 1000, 2000, 5000, 10000),
    val shareApproximateLocation: Boolean = false,
    val shareBattery: Boolean = true,
    val shareSpeed: Boolean = true,
    val shareElevation: Boolean = true,
    val devicesLimit: Int = 0,
    val devicesCount: Int = 0,
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
    val canShare: Boolean = errors.isEmpty() && groupState.data != null && groupState.data?.uiType != GroupItemState.GroupUiType.UNVERIFIED
}

sealed class SharingStartScreenEvent() {
    data class Error(val error: Throwable): SharingStartScreenEvent()
    object Started: SharingStartScreenEvent()
    object SelectGroupInfo: SharingStartScreenEvent()
    object UnverifiedGroup: SharingStartScreenEvent()
    data class DevicesLimitReached(val limit: Int) : SharingStartScreenEvent()
    object LocationStatusInsufficient: SharingStartScreenEvent()
    object MissingBackgroundPermission: SharingStartScreenEvent()
    object CanNotRegisterDevice: SharingStartScreenEvent()
}
