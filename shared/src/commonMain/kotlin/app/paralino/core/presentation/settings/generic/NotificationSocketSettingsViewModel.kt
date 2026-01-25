package app.paralino.core.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.settings.SettingsActionItemType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class NotificationSocketSettingsViewModel(
    private val serviceManager: NotificationSocketServiceManager,
    deviceApiProvider: IDeviceApiProvider
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_notificationSocket_title.localizedString(),
    deviceApiProvider = deviceApiProvider
) {

    init {
        deviceApiProvider.notificationStatusProvider.status
            .onEach { status ->
                if (status.isNotificationPermissionAllowed) {
                    _uiState.update { createInitialState() }
                } else {
                    _uiState.update { getMissingPermissionEmpty() }
                }
            }
            .launchIn(coroutineScope)
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val isServiceEnabled = serviceManager.isServiceEnabledInSettings()
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = listOf(
                    GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
                    GenericSettingListItem.Subtitle(MR.strings.setting_notificationSocket_info.localizedString()),
                    GenericSettingListItem.Switch(
                        key = "switchNotificationSocket",
                        keyId = "switchNotificationSocket",
                        title = MR.strings.setting_notificationSocket_switch_title.localizedString(),
                        description = if (isServiceEnabled) {
                            MR.strings.setting_notificationSocket_switch_info_on.localizedString()
                        } else {
                            MR.strings.setting_notificationSocket_switch_info_off.localizedString()
                        },
                        icon = CoreAppIcons.NotificationsActive,
                        isChecked = isServiceEnabled,
                        isEnabled = true,
                        isLoading = false,
                        isVisible = true
                    ),
                    GenericSettingListItem.SettingsActionItem(
                        keyId = "channelSettings",
                        text = MR.strings.setting_notificationSocket_channelSettings_title.localizedString(),
                        icon = CoreAppIcons.PhonelinkSetup,
                        type = SettingsActionItemType.Navigation,
                        isVisible = isServiceEnabled,
                        tag = null,
                        description = null
                    )
                )
            )
        )
    }

    private fun getMissingPermissionEmpty(): GenericSettingsScreenState {
        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.EmptyState(
                icon = CoreAppIcons.NotificationsOff,
                content = MR.strings.setting_notificationSocket_notificationPermission_error.localizedString(),
                buttonText = MR.strings.button_continue.localizedString(),
                buttonLoading = null
            )
        )
    }

    override fun handleUiEvent(event: GenericSettingsScreenUiEvent) {
        when (event) {
            is GenericSettingsScreenUiEvent.ToggleSwitch<*> -> {
                when (event.switch.keyId) {
                    "switchNotificationSocket" -> {
                        toggleSocketSwitch(switch = event.switch)
                    }
                }
            }

            is GenericSettingsScreenUiEvent.SelectActionItem -> {
                when (event.item.keyId) {
                    "channelSettings" -> {
                        _eventChannel.trySend(GenericSettingsScreenEvent.OpenNotificationSocketChannelSettings)
                    }
                }
            }

            is GenericSettingsScreenUiEvent.EmptyStateActionClick -> {
                _eventChannel.trySend(GenericSettingsScreenEvent.AskForNotificationPermission)
            }

            else -> {}
        }
    }

    private fun toggleSocketSwitch(switch: GenericSettingListItem.Switch<*>) {
        val isEnabled = !switch.isChecked
        serviceManager.setServiceEnabledState(enabledStatus = isEnabled)

        _uiState.update { state ->
            state.copy(
                content = when (state.content) {
                    is GenericSettingsScreenStateContent.ListItems -> {
                        state.content.copy(
                            items = state.content.items.map { item ->
                                when (val finalItem = item) {
                                    is GenericSettingListItem.Switch<*> -> {
                                        when (finalItem.keyId) {
                                            "switchNotificationSocket" -> {
                                                finalItem.copy(
                                                    isChecked = isEnabled,
                                                    description = if (isEnabled) {
                                                        MR.strings.setting_notificationSocket_switch_info_on.localizedString()
                                                    } else {
                                                        MR.strings.setting_notificationSocket_switch_info_off.localizedString()
                                                    }
                                                )
                                            }

                                            else -> item
                                        }
                                    }

                                    is GenericSettingListItem.SettingsActionItem -> {
                                        when (finalItem.keyId) {
                                            "channelSettings" -> {
                                                finalItem.copy(isVisible = isEnabled)
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
