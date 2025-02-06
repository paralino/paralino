package app.paralino.core.presentation.settings.location

import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class LocationSettingsScreenState(
    val highPerformanceSettings: LocationSettingsHighPerformance,
    val batterySaverSettings: LocationSettingsBatterySaver,
    val balancedSettings: LocationSettingsBalanced,
    val alertDialog: MainAlertDialog?
)

data class LocationSettingsHighPerformance(
    val items: List<LocationSettingsItemState>,
) {
    val selectedItem: LocationSettingsItemState = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.performance.key == LocationPerformanceSettingsType.HighPerformance.default.key }
        ?: items.first()
}

data class LocationSettingsBatterySaver(
    val activeOnLowPowerMode: Boolean,
    val items: List<LocationSettingsItemState>,
) {
    val selectedItem: LocationSettingsItemState = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.performance.key == LocationPerformanceSettingsType.BatterySaver.default.key }
        ?: items.first()
}

data class LocationSettingsBalanced(
    val useHighPerformanceLocation: Boolean,
    val showLocationIndicator: Boolean?,
    val pausesLocationAutomatically: Boolean?,
    val items: List<LocationSettingsItemState>
) {
    val selectedItem: LocationSettingsItemState = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.performance.key == LocationPerformanceSettingsType.Balanced.default.key }
        ?: items.first()
}

data class LocationSettingsItemState(
    val performance: LocationPerformanceSettingsType,
    val title: String,
    val description: String,
    val batteryImpact: String?,
    val highBatteryImpact: Boolean,
    val isPremiumFeatureEnabled: Boolean,
    val isSelected: Boolean
)

sealed interface LocationSettingsScreenEvent {
    data object OpenPaywall : LocationSettingsScreenEvent
}

sealed interface LocationSettingsScreenUiEvent {
    data object ToggleBalancedAutoPause : LocationSettingsScreenUiEvent
    data object ToggleBalancedShowIndicator : LocationSettingsScreenUiEvent
    data object ToggleBalancedHighPerformance : LocationSettingsScreenUiEvent
    data object ToggleFollowDeviceLowPowerMode : LocationSettingsScreenUiEvent

    data class ChangePerformance(val type: LocationPerformanceSettingsType) :
        LocationSettingsScreenUiEvent

    data class DialogAction(val action: AlertDialogAction) : LocationSettingsScreenUiEvent
}