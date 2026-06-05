package app.paralino.core.presentation.settings.location

import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.repo.location.LocationPerformanceSettingsType

data class LocationSettingsScreenState(
    val highPerformanceSettings: LocationSettingsHighPerformance,
    val batterySaverSettings: LocationSettingsBatterySaver,
    val balancedSettings: LocationSettingsBalanced,
    val historySettings: LocationSettingsHistory,
    val alertDialog: MainAlertDialog?
)

data class LocationSettingsHighPerformance(
    val items: List<GroupOptionItem>,
)

data class LocationSettingsBatterySaver(
    val activeOnLowPowerMode: Boolean,
    val items: List<GroupOptionItem>,
) {
    val selectedItem: GroupOptionItem = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.id == LocationPerformanceSettingsType.BatterySaver.default.key }
        ?: items.first()
}

data class LocationSettingsBalanced(
    val useHighPerformanceLocation: Boolean,
    val showLocationIndicator: Boolean?,
    val pausesLocationAutomatically: Boolean?,
    val items: List<GroupOptionItem>
) {
    val selectedItem: GroupOptionItem = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.id == LocationPerformanceSettingsType.Balanced.default.key }
        ?: items.first()
}

data class LocationSettingsHistory(
    val items: List<GroupOptionItem>,
) {
    val selectedItem: GroupOptionItem = items.firstOrNull { it.isSelected }
        ?: items.firstOrNull() { it.id == LocationPerformanceSettingsType.History.default.key }
        ?: items.first()
}

//data class LocationSettingsItemState(
//    val performance: LocationPerformanceSettingsType,
//    val title: String,
//    val description: String,
//    val batteryImpact: String?,
//    val highBatteryImpact: Boolean,
//    val isPremiumFeatureEnabled: Boolean,
//    val isSelected: Boolean
//)

sealed interface LocationSettingsScreenEvent {
    data object OpenPaywall : LocationSettingsScreenEvent
}

sealed interface LocationSettingsScreenUiEvent {
    data object ToggleBalancedAutoPause : LocationSettingsScreenUiEvent
    data object ToggleBalancedShowIndicator : LocationSettingsScreenUiEvent
    data object ToggleBalancedHighPerformance : LocationSettingsScreenUiEvent
    data object ToggleFollowDeviceLowPowerMode : LocationSettingsScreenUiEvent

    data class ChangePerformance(val option: GroupOptionItem) :
        LocationSettingsScreenUiEvent

    data class DialogAction(val action: AlertDialogAction) : LocationSettingsScreenUiEvent
}