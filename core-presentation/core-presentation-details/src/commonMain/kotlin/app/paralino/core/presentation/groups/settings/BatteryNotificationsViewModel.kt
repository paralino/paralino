package app.paralino.core.presentation.groups.settings

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.GroupBatterySettingsUpdateUseCase
import app.paralino.core.domain.user.PersonalBatterySettingsUpdateUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsUserEntry
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class BatteryNotificationsViewModel(
    val groupId: String,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val groupBatterySettingsUpdate: GroupBatterySettingsUpdateUseCase,
    private val personalBatterySettingsUpdate: PersonalBatterySettingsUpdateUseCase,
    private val appPrefs: AppPrefs,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource
) : CoreViewModel() {

    private val isPersonalSpace = groupId.asGroupOrSpaceId().isPersonalSpace
    private val groupData = MutableStateFlow<UserGroupWithMembers?>(null)
    private val _uiState =
        MutableStateFlow<Resource<BatteryNotificationScreenState>>(Resource.NotInitialized)
    val uiState: StateFlow<Resource<BatteryNotificationScreenState>> = _uiState

    private val _eventChannel = Channel<BatteryNotificationScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        if (isPersonalSpace) {
            personalSpaceLocal.personalSpaceState
                .mapNotNull { it.data }
                .map { personal ->
                    val currentSettings = personal.currentDevice?.batteryAlertSettings
                    BatteryNotificationScreenState(
                        groupId = groupId.asGroupOrSpaceId(),
                        groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
                        groupImage = personal.user.data.data?.userImage
                            ?: ImageModel.emoji(Emojis.robot),
                        fullLoading = false,
                        isActive = currentSettings?.isActive != false,
                        isActiveSwitchLoading = false,
                        settings = personal.devices
                            .map { device ->
                                val deviceSettings = currentSettings?.settings?.firstOrNull {
                                    it.deviceId == device.deviceId
                                }
                                BatteryNotificationMemberState(
                                    groupId = groupId,
                                    alertItemId = device.deviceId,
                                    isCurrentUserOrDevice = device.deviceId == appPrefs.deviceId,
                                    name = device.deviceNameOrNull ?: device.deviceType?.toString() ?: "?",
                                    image = ImageModel.emoji(Emojis.mobilePhone),
                                    tag = device.deviceType?.toString(),
                                    notify = deviceSettings?.isActive ?: (device.deviceId != appPrefs.deviceId),
                                    loadingNotify = false
                                )
                            }.sortedSettings()
                    )
                }
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        is Payload.Success -> _uiState.update { Resource.Ready(result.data) }
                        is Payload.Error -> showSnackbar(result.exception.errorString())
                        Payload.Loading -> {}
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(coroutineScope)
        } else {
            groupLocalDataSource.getGroupWithMembers(groupId)
                .filterNotNull()
                .map { group ->
                    groupData.update { group }

                    val currentMemberSettings = group.memberByUserId(userId = appPrefs.userId)
                        ?.member?.settings!!

                    BatteryNotificationScreenState(
                        groupId = groupId.asGroupOrSpaceId(),
                        groupName = group.name,
                        groupImage = group.image,
                        fullLoading = false,
                        isActive = currentMemberSettings.batteryAlertSettings?.isActive != false,
                        isActiveSwitchLoading = false,
                        settings = group.members.map { member ->
                            val memberSettings = currentMemberSettings
                                .batteryAlertSettings
                                ?.settings
                                ?.firstOrNull { it.memberUserId == member.member.userId }
                            BatteryNotificationMemberState(
                                groupId = groupId,
                                alertItemId = member.member.userId,
                                isCurrentUserOrDevice = member.member.userId == appPrefs.userId,
                                name = member.member.name,
                                image = member.member.image ?: ImageModel.emoji(Emojis.robot),
                                tag = null,
                                notify = memberSettings?.isActive
                                    ?: (member.member.userId != appPrefs.userId),
                                loadingNotify = false
                            )
                        }.sortedSettings()
                    )
                }
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        is Payload.Success -> {
                            _uiState.update { Resource.Ready(result.data) }
                        }

                        is Payload.Error -> {
                            if (result.exception.asRemoteException() is NotFoundException) {
                                showSnackbar(MR.strings.group_load_error.localizedString())
                            } else {
                                showSnackbar(result.exception.errorString())
                            }
                        }

                        Payload.Loading -> {}
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(coroutineScope)
        }
    }

    fun onEvent(event: BatteryNotificationScreenUiEvent) {
        when (event) {
            is BatteryNotificationScreenUiEvent.ToggleBatterySettingForItem -> {
                toggleSettingForItem(alertItemId = event.alertItemId)
            }

            BatteryNotificationScreenUiEvent.ToggleBatterySettingForGroup -> {
                toggleSettingForGroup()
            }
        }
    }

    private fun toggleSettingForGroup() {
        val state = _uiState.value.data ?: return
        if (state.fullLoading) return
        if (state.isActiveSwitchLoading) return
        if (state.settings.any { it.loading }) return
        if (groupBatterySettingsUpdate.isProcessingAny()) return
        if (personalBatterySettingsUpdate.isProcessingAny()) return

        val newSetting = !state.isActive

        val flow = if (isPersonalSpace) {
            personalBatterySettingsUpdate.updateBatterySettings(
                active = newSetting
            )
                .mapToPayload { it }
        } else {
            val group = groupData.value?.group ?: return
            groupBatterySettingsUpdate.updateBatterySettings(
                group = group,
                active = newSetting
            )
                .mapToPayload { it }
        }

        flow
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapReady {
                        it.copy(isActiveSwitchLoading = result is Payload.Loading)
                    }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> {}
                    is Payload.Error -> {
                        messageDispatcher.showSnackbar(result.exception.errorString())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun toggleSettingForItem(alertItemId: String) {
        val state = _uiState.value.data ?: return
        if (state.fullLoading) return
        if (state.isActiveSwitchLoading) return
        if (state.settings.any { it.loading }) return
        if (groupBatterySettingsUpdate.isProcessingAny()) return

        if (!state.isActive) return

        val setting = state.settings.firstOrNull { it.alertItemId == alertItemId } ?: return

        val flow = if (isPersonalSpace) {
            val newSetting = BatteryNotificationsSettingsUserEntry(
                memberUserId = appPrefs.userId,
                deviceId = setting.alertItemId,
                isActive = !setting.notify
            )
            personalBatterySettingsUpdate.updateBatterySettings(
                data = newSetting
            )
                .mapToPayload { it }
        } else {
            val newSetting = BatteryNotificationsSettingsUserEntry(
                memberUserId = setting.alertItemId,
                isActive = !setting.notify
            )

            val group = groupData.value?.group ?: return
            groupBatterySettingsUpdate.updateBatterySettings(
                group = group,
                data = newSetting
            )
                .mapToPayload { it }
        }

        flow
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapReady {
                        it.copy(
                            settings = it.settings.map {
                                if (it.alertItemId == alertItemId) {
                                    it.copy(loadingNotify = result is Payload.Loading)
                                } else {
                                    it
                                }
                            }
                        )
                    }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> {}
                    is Payload.Error -> {
                        messageDispatcher.showSnackbar(result.exception.errorString())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun List<BatteryNotificationMemberState>.sortedSettings(): List<BatteryNotificationMemberState> {
        return this
            .sortedBy { it.name }
            .sortedBy { it.isCurrentUserOrDevice }
    }
}