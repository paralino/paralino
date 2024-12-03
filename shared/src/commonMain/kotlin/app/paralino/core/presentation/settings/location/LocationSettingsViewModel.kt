package app.paralino.core.presentation.settings.location

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.PlanType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationSettingsViewModel(
    private val appPrefs: AppPrefs,
    private val userRepository: UserRepository,
    private val locationPerformanceManager: LocationPerformanceManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        LocationSettingsScreenState(
            highPerformanceSettings = LocationSettingsHighPerformance(
                items = listOfNotNull(
                    LocationPerformanceSettingsType.HighPerformance.Level1,
                    LocationPerformanceSettingsType.HighPerformance.Level2,
                    LocationPerformanceSettingsType.HighPerformance.Level3,
                    LocationPerformanceSettingsType.HighPerformance.LevelMax.takeIf {
                        AppBuildConfig.instance.hasPreviewFeatures
                    }
                ).map { level ->
                    LocationSettingsItemState(
                        performance = level,
                        isPremiumFeatureEnabled = !isUserOnFreePlan() || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        batteryImpact = getOptionBatteryImpact(level),
                        highBatteryImpact = level is LocationPerformanceSettingsType.HighPerformance.LevelMax || level is LocationPerformanceSettingsType.HighPerformance.Level3,
                        isSelected = level.key == (appPrefs.locationSettingsHighPerformanceDurationType
                            ?: LocationPerformanceSettingsType.HighPerformance.default.key)
                    )
                }
            ),
            batterySaverSettings = LocationSettingsBatterySaver(
                items = listOf(
                    LocationPerformanceSettingsType.BatterySaver.Level1,
                    LocationPerformanceSettingsType.BatterySaver.Level2,
                    LocationPerformanceSettingsType.BatterySaver.Level3
                ).map { level ->
                    LocationSettingsItemState(
                        performance = level,
                        isPremiumFeatureEnabled = !isUserOnFreePlan() || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        batteryImpact = getOptionBatteryImpact(level),
                        highBatteryImpact = level is LocationPerformanceSettingsType.BatterySaver.Level3,
                        isSelected = level.key == (appPrefs.locationSettingsBatterySaverPerformanceType
                            ?: LocationPerformanceSettingsType.BatterySaver.default.key)
                    )
                }
            ),
            balancedSettings = LocationSettingsBalanced(
                useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy,
                items = listOf(
                    LocationPerformanceSettingsType.Balanced.Level1,
                    LocationPerformanceSettingsType.Balanced.Level2,
                    LocationPerformanceSettingsType.Balanced.Level3,
                    LocationPerformanceSettingsType.Balanced.Level4,
                    LocationPerformanceSettingsType.Balanced.Level5
                ).map { level ->
                    LocationSettingsItemState(
                        performance = level,
                        isPremiumFeatureEnabled = !isUserOnFreePlan() || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        batteryImpact = getOptionBatteryImpact(level),
                        highBatteryImpact = level is LocationPerformanceSettingsType.Balanced.Level5,
                        isSelected = level.key == (appPrefs.locationSettingsBalancedPerformanceType
                            ?: LocationPerformanceSettingsType.Balanced.default.key)
                    )
                }
            ),
            alertDialog = null
        )
    )

    val uiState: StateFlow<LocationSettingsScreenState> = _uiState

    private val _eventChannel = Channel<LocationSettingsScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun onEvent(event: LocationSettingsScreenUiEvent) {
        when (event) {
            is LocationSettingsScreenUiEvent.ToggleBalancedHighPerformance -> {
                val newValue = !appPrefs.locationSettingsBalancedHighAccuracy
                appPrefs.locationSettingsBalancedHighAccuracy = newValue
                val newSettings = uiState.value.balancedSettings.copy(
                    useHighPerformanceLocation = newValue
                )
                _uiState.update {
                    it.copy(balancedSettings = newSettings)
                }
                locationPerformanceManager.updateLocationSettings(
                    useHighPerformanceLocation = newSettings.useHighPerformanceLocation,
                    balancedPerformanceSettings = newSettings.selectedItem.performance,
                    batterySaverPerformanceSettings = uiState.value.batterySaverSettings.selectedItem.performance
                )
            }

            is LocationSettingsScreenUiEvent.ChangePerformance -> {

                if (event.type.isPremiumFeature && isUserOnFreePlan()) {
                    _uiState.update {
                        it.copy(alertDialog = MainAlertDialog.LocationPerformancePremiumInfo)
                    }
                    return
                }

                when (event.type) {
                    is LocationPerformanceSettingsType.HighPerformance -> {
                        appPrefs.locationSettingsHighPerformanceDurationType = event.type.key
                        val newSettings = uiState.value.highPerformanceSettings.copy(
                            items = uiState.value.highPerformanceSettings.items.map {
                                it.copy(isSelected = event.type.key == it.performance.key)
                            }
                        )
                        _uiState.update {
                            it.copy(highPerformanceSettings = newSettings)
                        }
                    }

                    is LocationPerformanceSettingsType.Balanced -> {
                        appPrefs.locationSettingsBalancedPerformanceType = event.type.key
                        val newSettings = uiState.value.balancedSettings.copy(
                            items = uiState.value.balancedSettings.items.map {
                                it.copy(isSelected = event.type.key == it.performance.key)
                            }
                        )
                        _uiState.update {
                            it.copy(balancedSettings = newSettings)
                        }
                        locationPerformanceManager.updateLocationSettings(
                            useHighPerformanceLocation = uiState.value.balancedSettings.useHighPerformanceLocation,
                            balancedPerformanceSettings = newSettings.selectedItem.performance,
                            batterySaverPerformanceSettings = uiState.value.batterySaverSettings.selectedItem.performance
                        )
                    }

                    is LocationPerformanceSettingsType.BatterySaver -> {
                        appPrefs.locationSettingsBatterySaverPerformanceType = event.type.key
                        val newSettings = uiState.value.batterySaverSettings.copy(
                            items = uiState.value.batterySaverSettings.items.map {
                                it.copy(isSelected = event.type.key == it.performance.key)
                            }
                        )
                        _uiState.update {
                            it.copy(batterySaverSettings = newSettings)
                        }
                        locationPerformanceManager.updateLocationSettings(
                            useHighPerformanceLocation = uiState.value.balancedSettings.useHighPerformanceLocation,
                            balancedPerformanceSettings = uiState.value.balancedSettings.selectedItem.performance,
                            batterySaverPerformanceSettings = newSettings.selectedItem.performance
                        )
                    }
                }
            }

            is LocationSettingsScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.LocationPerformancePremiumInfo -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            coroutineScope.launch {
                                _eventChannel.send(LocationSettingsScreenEvent.OpenPaywall)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun isUserOnFreePlan(): Boolean {
        return userRepository.userPlans.value.highestValidPlanConfig.planTypeEnum == PlanType.FREE
//        return !userRepository.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
    }

    private fun getOptionTitle(level: LocationPerformanceSettingsType): String {
        return when (level) {
            is LocationPerformanceSettingsType.HighPerformance.Level1 -> MR.strings.setting_location_highPerformance_level_1.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.Level2 -> MR.strings.setting_location_highPerformance_level_2.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.Level3 -> MR.strings.setting_location_highPerformance_level_3.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.LevelMax -> MR.strings.setting_location_highPerformance_level_4.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level1 -> MR.strings.setting_location_balanced_level_1.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level2 -> MR.strings.setting_location_balanced_level_2.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level3 -> MR.strings.setting_location_balanced_level_3.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level4 -> MR.strings.setting_location_balanced_level_4.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level5 -> MR.strings.setting_location_balanced_level_5.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level1 -> MR.strings.setting_location_batterySaver_level_1.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level2 -> MR.strings.setting_location_batterySaver_level_2.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level3 -> MR.strings.setting_location_batterySaver_level_3.localizedString()
        }
    }

    private fun getOptionDescription(level: LocationPerformanceSettingsType): String {
        return when (level) {
            is LocationPerformanceSettingsType.HighPerformance.Level1 -> MR.strings.setting_location_highPerformance_level_1_info.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.Level2 -> MR.strings.setting_location_highPerformance_level_2_info.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.Level3 -> MR.strings.setting_location_highPerformance_level_3_info.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.LevelMax -> MR.strings.setting_location_highPerformance_level_4_info.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level1 -> MR.strings.setting_location_balanced_level_1_info.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level2 -> MR.strings.setting_location_balanced_level_2_info.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level3 -> MR.strings.setting_location_balanced_level_3_info.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level4 -> MR.strings.setting_location_balanced_level_4_info.localizedString()
            is LocationPerformanceSettingsType.Balanced.Level5 -> MR.strings.setting_location_balanced_level_5_info.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level1 -> MR.strings.setting_location_batterySaver_level_1_info.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level2 -> MR.strings.setting_location_batterySaver_level_2_info.localizedString()
            is LocationPerformanceSettingsType.BatterySaver.Level3 -> MR.strings.setting_location_batterySaver_level_3_info.localizedString()
        }
    }

    private fun getOptionBatteryImpact(level: LocationPerformanceSettingsType): String? {
        return when (level) {
            is LocationPerformanceSettingsType.HighPerformance.Level1,
            is LocationPerformanceSettingsType.HighPerformance.Level2 -> null

            is LocationPerformanceSettingsType.HighPerformance.Level3 -> MR.strings.setting_location_batteryImpact_level_4.localizedString()
            is LocationPerformanceSettingsType.HighPerformance.LevelMax -> MR.strings.setting_location_batteryImpact_level_5.localizedString()

            is LocationPerformanceSettingsType.Balanced.Level1,
            is LocationPerformanceSettingsType.Balanced.Level2,
            is LocationPerformanceSettingsType.Balanced.Level3,
            is LocationPerformanceSettingsType.Balanced.Level4 -> null

            is LocationPerformanceSettingsType.Balanced.Level5 -> MR.strings.setting_location_batteryImpact_level_5.localizedString()

            is LocationPerformanceSettingsType.BatterySaver.Level1,
            is LocationPerformanceSettingsType.BatterySaver.Level2 -> null

            is LocationPerformanceSettingsType.BatterySaver.Level3 -> MR.strings.setting_location_batteryImpact_level_4.localizedString()
        }
    }
}