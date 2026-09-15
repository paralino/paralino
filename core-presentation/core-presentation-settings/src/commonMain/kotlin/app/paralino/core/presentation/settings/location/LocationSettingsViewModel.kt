package app.paralino.core.presentation.settings.location

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.provider.current
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationSettingsViewModel(
    private val appPrefs: AppPrefs,
    private val appConfig: AppConfigRepository,
    private val userRepository: UserRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val premiumStatusManager: PremiumStatusManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<LocationSettingsScreenState>(createInitialState())
    val uiState: StateFlow<LocationSettingsScreenState> = _uiState

    private val _eventChannel = Channel<LocationSettingsScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch {
            try {
                premiumStatusManager.adjustDeviceSettingsForPlan()
                _uiState.update { createInitialState() }
            } catch (e: Exception) {
            }
        }
    }

    fun onEvent(event: LocationSettingsScreenUiEvent) {
        when (event) {
            is LocationSettingsScreenUiEvent.ToggleBalancedHighPerformance -> toggleBalancedHighPerformance()
            is LocationSettingsScreenUiEvent.ToggleFollowDeviceLowPowerMode -> toggleFollowLowPowerMode()
            is LocationSettingsScreenUiEvent.ChangePerformance -> changeLocationPerformance(event)
            LocationSettingsScreenUiEvent.ToggleBalancedAutoPause -> toggleIOSAutoPause()
            LocationSettingsScreenUiEvent.ToggleBalancedShowIndicator -> toggleIOSLocationIndicator()

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

    private fun createInitialState(): LocationSettingsScreenState {
        val canHavePremiumModes = canUserHavePremiumModes()
        return LocationSettingsScreenState(
            highPerformanceSettings = LocationSettingsHighPerformance(
                items = listOfNotNull(
                    LocationPerformanceSettingsType.HighPerformance.Level1,
                    LocationPerformanceSettingsType.HighPerformance.Level2,
                    LocationPerformanceSettingsType.HighPerformance.Level3,
                    LocationPerformanceSettingsType.HighPerformance.LevelMax.takeIf {
                        appConfig.isInAppDevMode
                    }
                ).map { level ->
                    GroupOptionItem(
                        key = level,
                        id = level.key,
                        isPremiumFeatureEnabled = canHavePremiumModes || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        tag = getOptionBatteryImpact(level),
                        isTagWarning = level is LocationPerformanceSettingsType.HighPerformance.LevelMax || level is LocationPerformanceSettingsType.HighPerformance.Level3,
                        isSelected = level.key == (
                                appPrefs.locationSettingsHighPerformanceDurationType
                                    ?: appPrefs.locationSettingsHighPerformanceDurationTypeFallback
                                    ?: LocationPerformanceSettingsType.HighPerformance.default.key
                                )
                    )
                }
            ),
            batterySaverSettings = LocationSettingsBatterySaver(
                activeOnLowPowerMode = appPrefs.locationBatterySaverRespectsDeviceLowPower,
                items = listOf(
                    LocationPerformanceSettingsType.BatterySaver.Level1,
                    LocationPerformanceSettingsType.BatterySaver.Level2,
                    LocationPerformanceSettingsType.BatterySaver.Level3
                ).map { level ->
                    GroupOptionItem(
                        key = level,
                        id = level.key,
                        isPremiumFeatureEnabled = canHavePremiumModes || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        tag = getOptionBatteryImpact(level),
                        isTagWarning = level is LocationPerformanceSettingsType.BatterySaver.Level3,
                        isSelected = level.key == (appPrefs.locationSettingsBatterySaverPerformanceType
                            ?: LocationPerformanceSettingsType.BatterySaver.default.key)
                    )
                }
            ),
            balancedSettings = LocationSettingsBalanced(
                useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy,
                showLocationIndicator = when (DeviceType.current) {
                    DeviceType.Android -> null
                    DeviceType.iOS -> appPrefs.locationSettingsShowIndicatorIOS
                },
                pausesLocationAutomatically = when (DeviceType.current) {
                    DeviceType.Android -> null
                    DeviceType.iOS -> appPrefs.locationSettingsAutoPauseIOS
                },
                items = listOf(
                    LocationPerformanceSettingsType.Balanced.Level1,
                    LocationPerformanceSettingsType.Balanced.Level2,
                    LocationPerformanceSettingsType.Balanced.Level3,
                    LocationPerformanceSettingsType.Balanced.Level4,
                    LocationPerformanceSettingsType.Balanced.Level5
                ).map { level ->
                    GroupOptionItem(
                        key = level,
                        id = level.key,
                        isPremiumFeatureEnabled = canHavePremiumModes || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        tag = getOptionBatteryImpact(level),
                        isTagWarning = level is LocationPerformanceSettingsType.Balanced.Level5,
                        isSelected = level.key == (appPrefs.locationSettingsBalancedPerformanceType
                            ?: LocationPerformanceSettingsType.Balanced.default.key)
                    )
                }
            ),
            historySettings = LocationSettingsHistory(
                items = listOfNotNull(
                    LocationPerformanceSettingsType.History.Level1,
                    LocationPerformanceSettingsType.History.Level2,
                    LocationPerformanceSettingsType.History.Level3
                ).map { level ->
                    GroupOptionItem(
                        key = level,
                        id = level.key,
                        isPremiumFeatureEnabled = canHavePremiumModes || !level.isPremiumFeature,
                        title = getOptionTitle(level),
                        description = getOptionDescription(level),
                        tag = null,
                        isTagWarning = false,
                        isSelected = level.key == (appPrefs.locationSettingsHistoryPerformanceType
                            ?: LocationPerformanceSettingsType.History.default.key)
                    )
                }
            ),
            alertDialog = null
        )
    }

    private fun toggleIOSLocationIndicator() {
        val newValue = !appPrefs.locationSettingsShowIndicatorIOS
        appPrefs.locationSettingsShowIndicatorIOS = newValue
        val newSettings = uiState.value.balancedSettings.copy(
            showLocationIndicator = newValue
        )
        _uiState.update {
            it.copy(balancedSettings = newSettings)
        }
        locationPerformanceManager.updateLocationSettings {
            it.copy(showLocationIndicator = newValue)
        }
    }

    private fun toggleIOSAutoPause() {
        val newValue = !appPrefs.locationSettingsAutoPauseIOS
        appPrefs.locationSettingsAutoPauseIOS = newValue
        val newSettings = uiState.value.balancedSettings.copy(
            pausesLocationAutomatically = newValue
        )
        _uiState.update {
            it.copy(balancedSettings = newSettings)
        }
        locationPerformanceManager.updateLocationSettings {
            it.copy(pausesLocationAutomatically = newValue)
        }
    }

    private fun toggleBalancedHighPerformance() {
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
            balancedPerformanceSettings = newSettings.selectedItem.key as LocationPerformanceSettingsType.Balanced,
            batterySaverPerformanceSettings = uiState.value.batterySaverSettings.selectedItem.key as LocationPerformanceSettingsType.BatterySaver
        )
    }

    private fun toggleFollowLowPowerMode() {
        val newValue = !appPrefs.locationBatterySaverRespectsDeviceLowPower
        appPrefs.locationBatterySaverRespectsDeviceLowPower = newValue
        val newSettings = uiState.value.batterySaverSettings.copy(
            activeOnLowPowerMode = newValue
        )
        _uiState.update {
            it.copy(batterySaverSettings = newSettings)
        }
    }

    private fun changeLocationPerformance(event: LocationSettingsScreenUiEvent.ChangePerformance) {
        val type = event.option.key as LocationPerformanceSettingsType
        if (type.isPremiumFeature && !canUserHavePremiumModes()) {
            _uiState.update {
                it.copy(alertDialog = MainAlertDialog.LocationPerformancePremiumInfo)
            }
            return
        }

        when (type) {
            is LocationPerformanceSettingsType.HighPerformance -> {
                appPrefs.locationSettingsHighPerformanceDurationType = type.key
                val newSettings = uiState.value.highPerformanceSettings.copy(
                    items = uiState.value.highPerformanceSettings.items.map {
                        it.copy(isSelected = type.key == it.id)
                    }
                )
                _uiState.update {
                    it.copy(highPerformanceSettings = newSettings)
                }
            }

            is LocationPerformanceSettingsType.Balanced -> {
                appPrefs.locationSettingsBalancedPerformanceType = type.key
                val newSettings = uiState.value.balancedSettings.copy(
                    items = uiState.value.balancedSettings.items.map {
                        it.copy(isSelected = type.key == it.id)
                    }
                )
                _uiState.update {
                    it.copy(balancedSettings = newSettings)
                }
                locationPerformanceManager.updateLocationSettings(
                    useHighPerformanceLocation = uiState.value.balancedSettings.useHighPerformanceLocation,
                    balancedPerformanceSettings = newSettings.selectedItem.key as LocationPerformanceSettingsType.Balanced,
                    batterySaverPerformanceSettings = uiState.value.batterySaverSettings.selectedItem.key as LocationPerformanceSettingsType.BatterySaver
                )
            }

            is LocationPerformanceSettingsType.BatterySaver -> {
                appPrefs.locationSettingsBatterySaverPerformanceType = type.key
                val newSettings = uiState.value.batterySaverSettings.copy(
                    items = uiState.value.batterySaverSettings.items.map {
                        it.copy(isSelected = type.key == it.id)
                    }
                )
                _uiState.update {
                    it.copy(batterySaverSettings = newSettings)
                }
                locationPerformanceManager.updateLocationSettings(
                    useHighPerformanceLocation = uiState.value.balancedSettings.useHighPerformanceLocation,
                    balancedPerformanceSettings = uiState.value.balancedSettings.selectedItem.key as LocationPerformanceSettingsType.Balanced,
                    batterySaverPerformanceSettings = newSettings.selectedItem.key as LocationPerformanceSettingsType.BatterySaver
                )
            }

            is LocationPerformanceSettingsType.History -> {
                appPrefs.locationSettingsHistoryPerformanceType = type.key
                val newSettings = uiState.value.historySettings.copy(
                    items = uiState.value.historySettings.items.map {
                        it.copy(isSelected = type.key == it.id)
                    }
                )
                _uiState.update {
                    it.copy(historySettings = newSettings)
                }
            }
        }
    }

    private fun canUserHavePremiumModes(): Boolean {
        return userRepository.userPlans.value.highestValidPlanConfig.hasBestLocationPerformance ||
                groupLocalDataSource.allGroupsState.value.data?.any {
                    it.group.planConfig(remoteConfigRepo.getCachedConfigOnce()).hasBestLocationPerformance
                } ?: false
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
            LocationPerformanceSettingsType.History.Level1 -> MR.strings.setting_location_history_level_1.localizedString()
            LocationPerformanceSettingsType.History.Level2 -> MR.strings.setting_location_history_level_2.localizedString()
            LocationPerformanceSettingsType.History.Level3 -> MR.strings.setting_location_history_level_3.localizedString()
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
            LocationPerformanceSettingsType.History.Level1 -> MR.strings.setting_location_history_level_1_info.localizedString()
            LocationPerformanceSettingsType.History.Level2 -> MR.strings.setting_location_history_level_2_info.localizedString()
            LocationPerformanceSettingsType.History.Level3 -> MR.strings.setting_location_history_level_3_info.localizedString()
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

            LocationPerformanceSettingsType.History.Level1,
            LocationPerformanceSettingsType.History.Level2,
            LocationPerformanceSettingsType.History.Level3 -> null
        }
    }
}