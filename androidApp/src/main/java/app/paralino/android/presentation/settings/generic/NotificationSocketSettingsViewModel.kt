package app.paralino.android.presentation.settings.generic

import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenEvent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.db.AppPrefs
import app.paralino.android.provider.IUnifiedPushManager
import app.paralino.android.provider.UnifiedPushError
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.base.PushProvider
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.SettingsActionItemType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class NotificationSocketSettingsViewModel(
    private val serviceManager: NotificationSocketServiceManager,
    private val appPrefs: AppPrefs,
    private val appConfigRepo: AppConfigRepository,
    private val unifiedPushManager: IUnifiedPushManager,
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

        unifiedPushManager.errorState
            .onEach { error ->
                if (error != null) {
                    _uiState.update { state ->
                        state.copy(
                            alertDialog = MainAlertDialog.UnifiedPushRegistrationError(
                                reason = when (error) {
                                    is UnifiedPushError.Other -> error.message
                                    is UnifiedPushError.RegistrationFailed -> error.reason
                                }
                            )
                        )
                    }
                    unifiedPushManager.clearError()
                }
            }
            .launchIn(coroutineScope)
    }

    override fun createInitialState(): GenericSettingsScreenState {
        val isSocketEnabled = serviceManager.isServiceEnabledInSettings()
        val isUnifiedPushEnabled = !appPrefs.selectedUnifiedPushDistributor.isNullOrBlank()
        val defaultProvider = serviceManager.adjustedPushProvider()
        val isMasterToggleOn = when (defaultProvider) {
            PushProvider.NOT_SET -> false
            PushProvider.DEVICE_NATIVE -> {
                if (appConfigRepo.appFlavor == AppFlavor.ANDROID_DIRECT) {
                    isSocketEnabled
                } else {
                    false
                }
            }
            PushProvider.PARALINO -> isSocketEnabled
            PushProvider.UNIFIED_PUSH -> isUnifiedPushEnabled
        }

        val distributors = unifiedPushManager.getDistributors()

        val items = mutableListOf<GenericSettingListItem>(
            GenericSettingListItem.Spacer(20.dp, keyId = "top_space"),
            GenericSettingListItem.Subtitle(MR.strings.setting_notificationSocket_info.localizedString()),
            GenericSettingListItem.Switch(
                key = "switchNotificationSocket",
                keyId = "switchNotificationSocket",
                title = MR.strings.setting_notificationSocket_switch_title.localizedString(),
                description = if (isMasterToggleOn) {
                    MR.strings.setting_notificationSocket_switch_info_on.localizedString()
                } else {
                    MR.strings.setting_notificationSocket_switch_info_off.localizedString()
                },
                icon = CoreAppIcons.NotificationsActive,
                isChecked = isMasterToggleOn,
                isEnabled = true,
                isLoading = false,
                isVisible = true
            )
        )

        if (distributors.isNotEmpty() && isMasterToggleOn) {
            val selectedDistributor = appPrefs.selectedUnifiedPushDistributor
            val options = mutableListOf(
                GroupOptionItem(
                    key = "socket",
                    id = "socket",
                    title = "Paralino",
                    description = null,
                    tag = null,
                    isTagWarning = false,
                    isPremiumFeatureEnabled = true,
                    isSelected = selectedDistributor.isNullOrEmpty()
                )
            )

            distributors.forEach { distributor ->
                val distributorName = when (distributor) {
                    "io.heckel.ntfy" -> "ntfy"
                    "org.unifiedpush.distributor.sunup" -> "Sunup"
                    "org.unifiedpush.distributor.nextpush" -> "NextPush"
                    else -> distributor
                }
                options.add(
                    GroupOptionItem(
                        key = distributor,
                        id = distributor,
                        title = distributorName,
                        description = if (distributor != distributorName) distributor else null,
                        tag = null,
                        isTagWarning = false,
                        isPremiumFeatureEnabled = true,
                        isSelected = distributor == selectedDistributor
                    )
                )
            }

            items.add(
                GenericSettingListItem.GroupOptions(
                    defaultId = "socket",
                    keyId = "pushProviderOptions",
                    items = options
                )
            )
        }

        items.add(
            GenericSettingListItem.SettingsActionItem(
                keyId = "channelSettings",
                text = MR.strings.setting_notificationSocket_channelSettings_title.localizedString(),
                icon = CoreAppIcons.PhonelinkSetup,
                type = SettingsActionItemType.Navigation,
                isVisible = isMasterToggleOn,
                tag = null,
                description = null
            )
        )

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                items = items
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

            is GenericSettingsScreenUiEvent.SelectOptionItem -> {
                if (event.groupOptions.keyId == "pushProviderOptions") {
                    handleProviderOptionSelection(event.option)
                }
            }

            is GenericSettingsScreenUiEvent.SelectActionItem -> {
                when (event.item.keyId) {
                    "channelSettings" -> {
                        _eventChannel.trySend(GenericSettingsScreenEvent.OpenNotificationSocketChannelSettings)
                    }
                }
            }

            is GenericSettingsScreenUiEvent.GlobalEmptyStateActionClick -> {
                _eventChannel.trySend(GenericSettingsScreenEvent.AskForNotificationPermission)
            }

            is GenericSettingsScreenUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
            }

            is GenericSettingsScreenUiEvent.EmbeddedEmptyStateActionClick -> {
                _eventChannel.trySend(GenericSettingsScreenEvent.AskForNotificationPermission)
            }
        }
    }

    private fun handleProviderOptionSelection(option: GroupOptionItem) {
        val distributor = option.key as String
        if (distributor == "socket") {
            unifiedPushManager.unregisterApp()
            serviceManager.setNotificationPushProvider(provider = PushProvider.PARALINO)
        } else {
            serviceManager.setNotificationPushProvider(provider = PushProvider.UNIFIED_PUSH)
            unifiedPushManager.registerApp(distributor)
        }
        _uiState.update { createInitialState() }
    }

    private fun toggleSocketSwitch(switch: GenericSettingListItem.Switch<*>) {
        val isEnabled = !switch.isChecked
        if (!isEnabled) {
            serviceManager.setNotificationPushProvider(provider = PushProvider.NOT_SET)
            unifiedPushManager.unregisterApp()
        } else {
            serviceManager.setNotificationPushProvider(provider = PushProvider.PARALINO)
        }

        _uiState.update { createInitialState() }
    }
}