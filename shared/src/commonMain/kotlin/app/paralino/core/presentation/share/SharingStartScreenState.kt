package app.paralino.core.presentation.share

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset

data class SharingStartScreenState constructor(
    val groupState: Payload<GroupItemState?> = Payload.Loading,
    val deviceName: String = "",
    val sharingTimeMillis: Long = SharingDurationPreset.premiumPresets.last(),
    val sharingPresetsMillis: List<Long> = SharingDurationPreset.premiumPresets,
    val approximateLocationMeters: Int = 200,
    val approximateLocationPresetsMeters: List<Int> = listOf(
        100, 200, 300, 500, 1000, 2000, 5000, 10000
    ),
    val shareApproximateLocation: Boolean = false,
    val shareBattery: Boolean = true,
    val shareSpeed: Boolean = true,
    val shareElevation: Boolean = true,
    val shareNetwork: Boolean = true,
    val notifyOthers: Boolean = true,
    val showNotifyOthers: Boolean = false,
    val isPremiumSectionVisible: Boolean = false,
    val devicesLimit: Int = 0,
    val devicesCount: Int = 0,
    val errors: List<ConditionErrorType> = emptyList(),
    val warnings: List<ConditionErrorType> = emptyList(),
    val loadingAction: Boolean = false,
    val alertDialog: MainAlertDialog? = null
) {
    val sharingTimeIndex: Int
        get() {
            return sharingPresetsMillis.indexOfFirst { it == sharingTimeMillis }.takeIf { it >= 0 }
                ?: sharingPresetsMillis.indexOfFirst { sharingTimeMillis < it }.takeIf { it >= 0 }
                ?: sharingPresetsMillis.lastIndex
        }

    val approximateLocationIndex: Int
        get() {
            return approximateLocationPresetsMeters.indexOfFirst { it == approximateLocationMeters }
                .takeIf { it >= 0 }
                ?: approximateLocationPresetsMeters.indexOfFirst { approximateLocationMeters < it }
                    .takeIf { it >= 0 }
                ?: approximateLocationPresetsMeters.lastIndex
        }

    val errorsAndWarnings = errors + warnings
    val isLongTerm: Boolean get() = LocationSharingContract.isLongTerm(sharingTimeMillis)
    val canShare: Boolean =
        errors.isEmpty() && groupState.data != null && groupState.data?.uiType != GroupItemState.GroupUiType.UNVERIFIED
}

sealed class SharingStartScreenEvent() {
    object Started : SharingStartScreenEvent()
    object OpenPaywall : SharingStartScreenEvent()
    object AdjustScrollPosition : SharingStartScreenEvent()
    data class AskForPermission(val error: ConditionErrorType) : SharingStartScreenEvent()
}

sealed interface SharingStartScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : SharingStartScreenUiEvent
}