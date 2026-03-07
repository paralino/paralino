package app.paralino.android.presentation.settings.generic

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import app.paralino.android.provider.IUnifiedPushManager
import app.paralino.android.provider.UnifiedPushError
import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.data
import app.paralino.core.models.user.PushProviderEndpointConfig
import app.paralino.core.presentation.settings.generic.BaseGenericSettingViewModel
import app.paralino.core.presentation.settings.generic.GenericSettingListItem
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenEvent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenState
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenStateContent
import app.paralino.core.presentation.settings.generic.GenericSettingsScreenUiEvent
import app.paralino.core.presentation.settings.generic.GroupOptionItem
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.SettingsActionItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationSocketSettingsViewModel(
    private val serviceManager: NotificationSocketServiceManager,
    private val appPrefs: AppPrefs,
    private val appConfigRepo: AppConfigRepository,
    private val unifiedPushManager: IUnifiedPushManager,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val pushProviderManager: IPushProviderManager,
    deviceApiProvider: IDeviceApiProvider
) : BaseGenericSettingViewModel(
    initialScreenTitle = MR.strings.setting_notificationSocket_title.localizedString(),
    deviceApiProvider = deviceApiProvider
) {

    private val loadingTrigger = MutableStateFlow(false)
    private var statePollJob: Job? = null

    init {
        loadingTrigger
            .onEach { isLoading ->
                _uiState.update {
                    it.copy(
                        content = when (val content = it.content) {
                            is GenericSettingsScreenStateContent.ListItems -> {
                                content.copy(isFullScreenLoading = isLoading)
                            }
                            else -> it.content
                        }
                    )
                }

                if (isLoading) {
                    pollForState()
                } else {
                    statePollJob?.cancel()
                    statePollJob = null
                }
            }
            .launchIn(viewModelScope)

        deviceApiProvider.notificationStatusProvider.status
            .onEach { status ->
                if (status.isNotificationPermissionAllowed) {
                    // adding artificial loading since fetching UnifiedPush distributors has small delay
                    // which is still small enough to make screen reload during entrance animation
                    // here we just delay the ui refresh a bit so it is not glitchy on low powered phones
                    delay(300)
                    val newState = createInitialState()
                    _uiState.update { newState }
                } else {
                    _uiState.update { getMissingPermissionEmpty() }
                }
            }
            .flowOn(Dispatchers.IO)
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
                    if (distributors.isEmpty()) {
                        MR.strings.setting_notificationSocket_providerParalino_info.localizedString()
                    } else {
                        MR.strings.setting_notificationSocket_switch_info_on.localizedString()
                    }
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
                    description = MR.strings.setting_notificationSocket_providerParalino_info.localizedString(),
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
                        tag = MR.strings.setting_notificationSocket_providerUnifiedPush_tag.localizedString(),
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

            items.add(GenericSettingListItem.Spacer(20.dp, keyId = "provider_space"))
        }

        if (defaultProvider == PushProvider.PARALINO) {
            val currentConnectionType = appPrefs.paralinoSocketConnectionType
            val connectionOptions = listOf(
                GroupOptionItem(
                    key = ParalinoSocketConnectionType.REALTIME,
                    id = ParalinoSocketConnectionType.REALTIME.key,
                    title = MR.strings.setting_notificationSocket_connectionType_realtime_title.localizedString(),
                    description = MR.strings.setting_notificationSocket_connectionType_realtime_info.localizedString(),
                    tag = MR.strings.setting_notificationSocket_connectionType_realtime_tag.localizedString(),
                    isTagWarning = true,
                    isPremiumFeatureEnabled = true,
                    isSelected = currentConnectionType == ParalinoSocketConnectionType.REALTIME
                ),
                GroupOptionItem(
                    key = ParalinoSocketConnectionType.PERIODIC_LEVEL_1,
                    id = ParalinoSocketConnectionType.PERIODIC_LEVEL_1.key,
                    title = MR.strings.setting_notificationSocket_connectionType_periodic1_title.localizedString(),
                    description = MR.strings.setting_notificationSocket_connectionType_periodic1_info.localizedString(),
                    tag = MR.strings.setting_notificationSocket_connectionType_periodic1_tag.localizedString(),
                    isTagWarning = true,
                    isPremiumFeatureEnabled = true,
                    isSelected = currentConnectionType == ParalinoSocketConnectionType.PERIODIC_LEVEL_1
                ),
                GroupOptionItem(
                    key = ParalinoSocketConnectionType.PERIODIC_LEVEL_2,
                    id = ParalinoSocketConnectionType.PERIODIC_LEVEL_2.key,
                    title = MR.strings.setting_notificationSocket_connectionType_periodic2_title.localizedString(),
                    description = MR.strings.setting_notificationSocket_connectionType_periodic2_info.localizedString(),
                    tag = MR.strings.setting_notificationSocket_connectionType_periodic2_tag.localizedString(),
                    isTagWarning = false,
                    isPremiumFeatureEnabled = true,
                    isSelected = currentConnectionType == ParalinoSocketConnectionType.PERIODIC_LEVEL_2
                ),
                GroupOptionItem(
                    key = ParalinoSocketConnectionType.PERIODIC_LEVEL_3,
                    id = ParalinoSocketConnectionType.PERIODIC_LEVEL_3.key,
                    title = MR.strings.setting_notificationSocket_connectionType_periodic3_title.localizedString(),
                    description = MR.strings.setting_notificationSocket_connectionType_periodic3_info.localizedString(),
                    tag = MR.strings.setting_notificationSocket_connectionType_periodic3_tag.localizedString(),
                    isTagWarning = false,
                    isPremiumFeatureEnabled = true,
                    isSelected = currentConnectionType == ParalinoSocketConnectionType.PERIODIC_LEVEL_3
                )
            )

            items.add(GenericSettingListItem.StickyHeader(keyId = "paralinoProviderSettings", content = MR.strings.setting_notificationSocket_connectionType_title.localizedString()))
            items.add(GenericSettingListItem.Subtitle(MR.strings.setting_notificationSocket_connectionType_info.localizedString()))

            items.add(
                GenericSettingListItem.GroupOptions(
                    defaultId = ParalinoSocketConnectionType.PERIODIC_LEVEL_2.key,
                    keyId = "paralinoConnectionTypeOptions",
                    items = connectionOptions
                )
            )
            items.add(GenericSettingListItem.Spacer(20.dp, keyId = "periodic_notice_space"))
            items.add(
                GenericSettingListItem.Notice(
                    keyId = "periodicRefreshNotice",
                    content = MR.strings.setting_notificationSocket_connectionType_notice.localizedString(),
                    icon = CoreAppIcons.LocationOn
                )
            )

            items.add(GenericSettingListItem.Spacer(20.dp, keyId = "system_settings_space"))

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

            items.add(GenericSettingListItem.Spacer(spacing = 40.dp, keyId = "bottom_space"))
        }

        return GenericSettingsScreenState(
            screenTitle = initialScreenTitle,
            alertDialog = null,
            content = GenericSettingsScreenStateContent.ListItems(
                isFullScreenLoading = loadingTrigger.value,
                loadingText = MR.strings.setting_notificationSocket_distributor_loading_text.localizedString(),
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

    private fun pollForState() {
        statePollJob?.cancel()
        statePollJob = coroutineScope.launch {
            var counter = 0
            while(isActive) {
                delay(3000)
                counter++
                val currentDevice = personalSpaceLocal.personalSpaceState.value.data?.currentDevice
                if (currentDevice != null) {
                    val syncedConfig = PushProviderEndpointConfig.get(currentDevice.pushProviderConfig)
                    val isConfigValid = if (currentDevice.pushProvider == PushProvider.PARALINO) {
                        syncedConfig != null
                    } else {
                        val localConfig = PushProviderEndpointConfig.get(appPrefs.unifiedPushEndpointConfigSynced)
                        syncedConfig != null && syncedConfig == localConfig
                    }
                    val isUpdated = currentDevice.pushProvider == appPrefs.notificationPushProvider && isConfigValid

                    if (!isUpdated) {
                        if (counter > 4) {
                            // on 5th pass we cancel
                            logg.tag("push_notifications").i("Device still not updated, cancel and show error")
                            loadingTrigger.update { false }
                            _uiState.update {
                                createInitialState()
                                    .copy(
                                        alertDialog = MainAlertDialog.UnifiedPushRegistrationError(
                                            reason = MR.strings.setting_notificationSocket_registration_error_message.localizedString()
                                        )
                                    )
                            }
                        } else if (counter > 2) {
                            // 3rd and 4th pass: try to sync tokens again, 2x
                            logg.tag("push_notifications").i("Device still not updated, trigger token sync")
                            pushProviderManager.syncPushToken()
                        } else {
                            // 1st and 2nd pass: wait and check again
                            logg.tag("push_notifications").i("Device still not updated, wait more")
                        }
                    } else {
                        logg.tag("push_notifications").i("Push provider registration complete")
                        loadingTrigger.update { false }
                    }
                } else {
                    loadingTrigger.update { counter > 2 }
                }
            }
        }
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
                } else if (event.groupOptions.keyId == "paralinoConnectionTypeOptions") {
                    val connectionType = event.option.key as ParalinoSocketConnectionType
                    appPrefs.paralinoSocketConnectionType = connectionType
                    
                    // We need to sync config immediately when changing options
                    pushProviderManager.syncPushToken()
                    
                    _uiState.update { createInitialState() }
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
            val wasAlreadySelected = serviceManager.adjustedPushProvider() == PushProvider.PARALINO
            loadingTrigger.update { !wasAlreadySelected }
            unifiedPushManager.unregisterApp()
            serviceManager.setNotificationPushProvider(provider = PushProvider.PARALINO)
        } else {
            loadingTrigger.update { true }
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
            loadingTrigger.update { true }
            serviceManager.setNotificationPushProvider(provider = PushProvider.PARALINO)
        }

        _uiState.update { createInitialState() }
    }

    override fun onCleared() {
        statePollJob?.cancel()
        super.onCleared()
    }
}