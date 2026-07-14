package app.paralino.core.presentation.share

import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset

data class SharingStartScreenState constructor(
    val groupState: Payload<GroupItemState?> = Payload.Loading,
    val canChangeGroup: Boolean,
    val deviceName: String? = null,
    val initialDeviceName: String? = null,
    val formattedEndTime: String? = null,
    val sharingTimeMillis: Long = SharingDurationPreset.premiumPresets.last(),
    val sharingPresetsMillis: List<Long> = SharingDurationPreset.premiumPresets,
    val approximateLocationPresets: List<ApproximateLocationDistancePair> = ApproximateLocationDistancePair.pairs,
    val approximateLocationValue: ApproximateLocationDistancePair = ApproximateLocationDistancePair.default,
    val shareApproximateLocation: Boolean = false,
    val formattedApproximateLocation: String? = "",
    val shareBattery: Boolean = true,
    val shareSpeed: Boolean = true,
    val shareElevation: Boolean = true,
    val shareNetwork: Boolean = true,
    val shareDeviceStatus: Boolean = true,
    val realtimeUpdatesState: Boolean? = null,
    val realtimeUpdatesDefaultState: Boolean? = null,
    val notifyOthers: Boolean = true,
    val saveToHistory: Boolean = true,
    val showNotifyOthers: Boolean = false,
    val showDeviceStatus: Boolean = false,
    val showRealtimeUpdatesStatus: Boolean = false,
    val showApproximateLocation: Boolean = false,
    val showDuration: Boolean = false,
    val showMetadata: Boolean = false,
    val isNotifyOtherEnabledInPlan: Boolean = true,
    val isRealtimeSharingEnabledInPlan: Boolean = true,
    val devicesLimit: Int = 0,
    val devicesCount: Int? = null,
    val premiumStatus: PremiumStatusLimitState? = null,
    val areLimitsExceeded: Boolean = false,
    val isDeviceRegistered: Boolean = true,
    val currentContract: LocationSharingContract? = null,
    val errors: List<ConditionErrorType> = emptyList(),
    val warnings: List<ConditionErrorType> = emptyList(),
    val loadingAction: Boolean = false,
    val alertDialog: MainAlertDialog? = null
) {
    val isNewContract: Boolean = currentContract == null
    val isNameChanged: Boolean = deviceName != initialDeviceName
    val isRealtimeOptionSelected: Boolean = if (!isRealtimeSharingEnabledInPlan) {
        false
    } else {
        realtimeUpdatesState ?: realtimeUpdatesDefaultState ?: true
    }
    val isTimelineReEnabled: Boolean = currentContract?.saveToHistory != true && saveToHistory
    val isRealtimeUpdatesReEnabled: Boolean = if (currentContract != null) {
        if (realtimeUpdatesDefaultState != null) {
            realtimeUpdatesDefaultState != isRealtimeOptionSelected
        } else {
            realtimeUpdatesState == false
        }
    } else {
        true
    }
    val canShowContent: Boolean = groupState is Payload.Loading || groupState.data != null

    val sharingTimeIndex: Int
        get() {
            return sharingPresetsMillis.indexOfFirst { it == sharingTimeMillis }.takeIf { it >= 0 }
                ?: sharingPresetsMillis.indexOfFirst { sharingTimeMillis < it }.takeIf { it >= 0 }
                ?: sharingPresetsMillis.lastIndex
        }

    val approximateLocationIndex: Int
        get() {
            return approximateLocationPresets.indexOfFirst { it.meters == approximateLocationValue.meters }
                .takeIf { it >= 0 }
                ?: approximateLocationPresets.indexOfFirst { approximateLocationValue.meters < it.meters }
                    .takeIf { it >= 0 }
                ?: approximateLocationPresets.lastIndex
        }

    val errorsAndWarnings = errors + warnings
    val isLongTerm: Boolean get() = LocationSharingContract.isLongTerm(sharingTimeMillis)
    val canShare: Boolean =
        errors.isEmpty() && groupState.data != null && groupState.data?.uiType != GroupItemState.GroupUiType.UNVERIFIED

    val isPersonalSpace = groupState.data?.isPersonalSpace == true
    val isOwner = groupState.data?.isCurrentUserOwner == true
    val isSpeedElevationAvailable: Boolean = !shareApproximateLocation
    val effectiveShareSpeed: Boolean = shareSpeed && isSpeedElevationAvailable
    val effectiveShareElevation: Boolean = shareElevation && isSpeedElevationAvailable

    val isDifferentThanContract: Boolean = currentContract?.let { contract ->
        contract.shareSpeed != shareSpeed ||
                contract.shareElevation != shareElevation ||
                contract.shareBattery != shareBattery ||
                contract.shareNetwork != shareNetwork ||
                contract.shareDeviceStatus != shareDeviceStatus ||
                contract.saveToHistory != saveToHistory ||
                isRealtimeUpdatesReEnabled ||
                isNameChanged ||
                (contract.approximateLocationInMeters != null) != shareApproximateLocation ||
                (shareApproximateLocation && contract.approximateLocationInMeters != approximateLocationValue.meters)
    } ?: true

    val isStartButtonEnabled =
        isDifferentThanContract && (premiumStatus is PremiumStatusLimitState.WithinLimits || isDeviceRegistered)
    val needsPlanUpgrade =
        premiumStatus is PremiumStatusLimitState.PlanLimitReached && !isDeviceRegistered
    val shouldUpgradePlan = isOwner && needsPlanUpgrade
    val shouldRegisterDevice = isNameChanged || isTimelineReEnabled || isRealtimeUpdatesReEnabled || isNewContract

    companion object {
        const val maxDeviceNameLength = 30
    }
}

sealed class SharingStartScreenEvent() {
    object Started : SharingStartScreenEvent()
    object Updated : SharingStartScreenEvent()
    object OpenPaywall : SharingStartScreenEvent()
    object AdjustScrollPosition : SharingStartScreenEvent()
    data class AskForPermission(val error: ConditionErrorType) : SharingStartScreenEvent()
}

sealed interface SharingStartScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : SharingStartScreenUiEvent
    data object StopSharing : SharingStartScreenUiEvent
    data object StartSharing : SharingStartScreenUiEvent
    data class ToggleNotifyOthers(val state: Boolean): SharingStartScreenUiEvent
    data class ToggleRealtimeUpdates(val state: Boolean): SharingStartScreenUiEvent
}