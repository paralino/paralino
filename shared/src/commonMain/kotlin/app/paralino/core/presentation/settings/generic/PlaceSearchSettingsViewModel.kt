package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import kotlinx.coroutines.flow.update

class PlaceSearchSettingsViewModel(
    private val appPrefs: AppPrefs,
    private val valueFormatter: IValueFormatter
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_placeSearch_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val currentOption = appPrefs.sendLocationWithPlaceSearch
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_placeSearch_info.localizedString()),
                    GenericSettingListItem.Switch(
                        key = "switchMapLocation",
                        keyId = "switchMapLocation",
                        title = MR.strings.setting_placeSearch_location_title.localizedString(),
                        description = MR.strings.setting_placeSearch_location_info.localizedString(),
                        icon = CoreAppIcons.Map,
                        isChecked = currentOption,
                        isEnabled = true,
                        isLoading = false,
                        isVisible = true
                    )
                )
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.DialogAction -> {}
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {
                when (event.switch.keyId) {
                    "switchMapLocation" -> {
                        toggleSearchSwitch(switch = event.switch)
                    }
                }
            }

            is GenericSettingsScreenUiEvent.SelectOptionItem -> {}
        }
    }

    private fun toggleSearchSwitch(switch: GenericSettingListItem.Switch<*>) {
        appPrefs.sendLocationWithPlaceSearch = !switch.isChecked

        _uiState.update { state ->
            state.copy(
                content = when (state.content) {
                    is GenericSettingsScreenStateContent.ListItems -> {
                        state.content.copy(
                            items = state.content.items.map { item ->
                                when (val finalItem = item) {
                                    is GenericSettingListItem.Switch<*> -> {
                                        when (finalItem.keyId) {
                                            "switchMapLocation" -> {
                                                finalItem.copy(
                                                    isChecked = !finalItem.isChecked
                                                )
                                            }

                                            else -> item
                                        }
                                    }

                                    else -> item
                                }
                            }
                        )
                    }

                    else -> state.content
                }
            )
        }
    }
}