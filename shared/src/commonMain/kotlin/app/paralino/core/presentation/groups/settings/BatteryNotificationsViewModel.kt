package app.paralino.core.presentation.groups.settings

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.GroupBatterySettingsUpdateUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsEntry
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class BatteryNotificationsViewModel(
    val groupId: String,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val batterySettingsUpdate: GroupBatterySettingsUpdateUseCase,
    private val remoteConfig: RemoteConfigRepository,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val groupData = MutableStateFlow<UserGroupWithMembers?>(null)
    private val _uiState =
        MutableStateFlow<Resource<BatteryNotificationScreenState>>(Resource.NotInitialized)
    val uiState: StateFlow<Resource<BatteryNotificationScreenState>> = _uiState

    private val _eventChannel = Channel<BatteryNotificationScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        groupLocalDataSource.getGroupWithMembers(groupId)
            .filterNotNull()
            .map { group ->
                groupData.update { group }

                val currentMemberSettings = group.memberByUserId(appPrefs.userId)?.settings!!

                BatteryNotificationScreenState(
                    groupId = groupId,
                    groupName = group.name,
                    groupImage = group.image,
                    fullLoading = false,
                    isGroupPremium = group.group.isGroupPremium(remoteConfig.getConfigOnce()),
                    canChangeBatterySettings = group.isCurrentUserOwner,
                    batteryPercentage = BatteryNotificationScreenState.defaultBatteryLevel,
                    isActive = currentMemberSettings.batteryAlertSettings?.isActive != false,
                    isActiveSwitchLoading = false,
                    settings = group.members.map { member ->
                        val memberSettings =
                            currentMemberSettings.batteryAlertSettings?.settings?.firstOrNull { it.memberUserId == member.member.userId }
                        BatteryNotificationMemberState(
                            groupId = groupId,
                            memberId = member.member.id,
                            memberUserId = member.member.userId,
                            isCurrentUser = member.member.userId == appPrefs.userId,
                            name = member.member.name,
                            image = member.member.image ?: ImageModel.emoji(Emojis.robot),
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

    fun onEvent(event: BatteryNotificationScreenUiEvent) {
        when (event) {
            is BatteryNotificationScreenUiEvent.ChangeDefaultBatteryLevel -> {
                _uiState.update {
                    it.mapReady {
                        it.copy(batteryPercentage = event.batteryLevel)
                    }
                }
            }

            is BatteryNotificationScreenUiEvent.ToggleBatterySettingForUser -> {
                toggleSettingForUser(event.userId)
            }

            BatteryNotificationScreenUiEvent.ToggleBatterySettingForGroup -> {
                toggleSettingForGroup()
            }
        }
    }

    private fun toggleSettingForGroup() {
        val state = _uiState.value.data ?: return
        val group = groupData.value?.group ?: return
        if (state.fullLoading) return
        if (state.isActiveSwitchLoading) return
        if (state.settings.any { it.loading }) return
        if (batterySettingsUpdate.isProcessingAny()) return

        val newSetting = !state.isActive
        batterySettingsUpdate.updateBatterySettings(
            group = group,
            active = newSetting
        )
            .mapToPayload { it }
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

    private fun toggleSettingForUser(userId: String) {
        val state = _uiState.value.data ?: return
        val group = groupData.value?.group ?: return
        if (state.fullLoading) return
        if (state.isActiveSwitchLoading) return
        if (state.settings.any { it.loading }) return
        if (batterySettingsUpdate.isProcessingAny()) return

        if (!state.isActive) return

        val setting = state.settings.firstOrNull { it.memberUserId == userId } ?: return
        val newSetting = BatteryNotificationsSettingsEntry(
            memberUserId = setting.memberUserId,
            isActive = !setting.notify
        )

        batterySettingsUpdate.updateBatterySettings(
            group = group,
            data = newSetting
        )
            .mapToPayload { it }
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapReady {
                        val newList = it.settings.toMutableList().apply {
                            removeAll { it.memberUserId == userId }
                            add(
                                setting.copy(
                                    loadingNotify = result is Payload.Loading
                                )
                            )
                        }.sortedSettings()
                        it.copy(settings = newList)
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
        return this.sortedBy { it.name }
            .sortedBy { it.memberUserId == appPrefs.userId }
    }
}