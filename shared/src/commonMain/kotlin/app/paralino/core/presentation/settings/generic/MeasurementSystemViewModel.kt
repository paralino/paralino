package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.MeasurementSystemType
import app.paralino.core.provider.localizedString
import kotlinx.coroutines.flow.update

class MeasurementSystemViewModel(
    private val appPrefs: AppPrefs,
    private val valueFormatter: IValueFormatter
): BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_measurementSystem_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.measurementSystem
        return GenericSettingsScreenState.ListItems(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            items = listOf(
                GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                GenericSettingListItem.GroupOptions(
                    defaultId = MeasurementSystemType.METRIC.key,
                    keyId = "units",
                    items = listOf(
                        GroupOptionItem(
                            key = null,
                            id = "system",
                            title = MR.strings.setting_measurementSystem_default_title.localizedString(),
                            description = valueFormatter.deviceDefaultMeasurementSystem.unitsInfo,
                            batteryImpact = null,
                            highBatteryImpact = false,
                            isPremiumFeatureEnabled = true,
                            isSelected = currentOption == null
                        ),
                        GroupOptionItem(
                            key = MeasurementSystemType.METRIC,
                            id = MeasurementSystemType.METRIC.key,
                            title = MR.strings.setting_measurementSystem_metric_title.localizedString(),
                            description = MeasurementSystemType.METRIC.unitsInfo,
                            batteryImpact = null,
                            highBatteryImpact = false,
                            isPremiumFeatureEnabled = true,
                            isSelected = currentOption == MeasurementSystemType.METRIC
                        ),
                        GroupOptionItem(
                            key = MeasurementSystemType.IMPERIAL,
                            id = MeasurementSystemType.IMPERIAL.key,
                            title = MR.strings.setting_measurementSystem_imperial_title.localizedString(),
                            description = MeasurementSystemType.IMPERIAL.unitsInfo,
                            batteryImpact = null,
                            highBatteryImpact = false,
                            isPremiumFeatureEnabled = true,
                            isSelected = currentOption == MeasurementSystemType.IMPERIAL
                        )
                    )
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DialogAction -> {}
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {}
            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                appPrefs.measurementSystem = event.option.key as? MeasurementSystemType?
                _uiState.update { state ->
                    when (state) {
                        is GenericSettingsScreenState.ListItems -> {
                            state.copy(
                                items = state.items.map { item ->
                                    when (val finalItem = item) {
                                        is GenericSettingListItem.GroupOptions -> {
                                            finalItem.copy(
                                                items = finalItem.items.map {
                                                    it.copy(isSelected = it.id == event.option.id)
                                                }
                                            )
                                        }
                                        else -> item
                                    }
                                }
                            )
                        }
                        else -> state
                    }
                }
            }
        }
    }
}