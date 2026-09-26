package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.domain.user.PersonalSpaceFeatureUseCase
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalSpaceFeatureSettingsViewModel(
    private val personalSpaceFeature: PersonalSpaceFeatureUseCase,
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_personalSpace_title.localizedString()
) {

    init {
        _uiState.update { createInitialState() }
    }

    override fun createInitialState(): GenericSettingsScreenState {
        return screenState(
            isChecked = personalSpaceFeature.isEnabled,
            isLoading = false,
            alertDialog = null
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {
                if (event.switch.keyId == switchKeyId) {
                    onToggle(isCurrentlyChecked = event.switch.isChecked)
                }
            }

            is GenericSettingsScreenUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
                if (event.action.dialog is MainAlertDialog.DisablePersonalSpaceOnDevice && event.action.isOk) {
                    disablePersonalSpace()
                }
            }

            else -> {}
        }
    }

    private fun onToggle(isCurrentlyChecked: Boolean) {
        if (!isCurrentlyChecked) {
            personalSpaceFeature.enable()
            updateSwitch(isChecked = true, isLoading = false)
            return
        }

        coroutineScope.launch {
            if (personalSpaceFeature.isCurrentDeviceActivated()) {
                _uiState.update { it.copy(alertDialog = MainAlertDialog.DisablePersonalSpaceOnDevice) }
            } else {
                disablePersonalSpace()
            }
        }
    }

    private fun disablePersonalSpace() {
        coroutineScope.launch {
            updateSwitch(isChecked = true, isLoading = true)
            personalSpaceFeature.disable()
            updateSwitch(isChecked = false, isLoading = false)
        }
    }

    private fun updateSwitch(isChecked: Boolean, isLoading: Boolean) {
        _uiState.update { state ->
            val content = state.content as? GenericSettingsScreenStateContent.ListItems
                ?: return@update screenState(isChecked = isChecked, isLoading = isLoading, alertDialog = state.alertDialog)
            state.copy(
                content = content.copy(
                    items = content.items.map { item ->
                        if (item is GenericSettingListItem.Switch<*> && item.keyId == switchKeyId) {
                            item.copy(isChecked = isChecked, isLoading = isLoading, isEnabled = !isLoading)
                        } else {
                            item
                        }
                    }
                )
            )
        }
    }

    private fun screenState(
        isChecked: Boolean,
        isLoading: Boolean,
        alertDialog: MainAlertDialog?,
    ): GenericSettingsScreenState {
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = alertDialog,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_personalSpace_info.localizedString()),
                    GenericSettingListItem.Switch(
                        key = switchKeyId,
                        keyId = switchKeyId,
                        title = MR.strings.setting_personalSpace_toggle_title.localizedString(),
                        description = MR.strings.setting_personalSpace_toggle_info.localizedString(),
                        icon = CoreAppIcons.PhoneAndroid,
                        isChecked = isChecked,
                        isEnabled = !isLoading,
                        isLoading = isLoading,
                        isVisible = true
                    ),
                    GenericSettingListItem.Spacer(spacing = 40.dp, keyId = "bottom_space")
                )
            )
        )
    }

    private companion object {
        const val switchKeyId = "switchPersonalSpace"
    }
}
