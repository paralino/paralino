package app.paralino.core.presentation.groups.settings

import app.paralino.core.resources.moko.*
import app.paralino.core.domain.group.GroupCreateUseCase
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.billing.FeatureTableItemValue
import app.paralino.core.presentation.billing.FeatureTableState
import app.paralino.core.presentation.billing.PlanInfoState
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.AppSnackbarPayload
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.MainAlertDialog.ToggleMemberVerification
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.exceptions.DataStillLoadingException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class GroupSettingsViewModel(
    val groupId: String,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val groupRemove: GroupRemoveUseCase,
    private val createGroup: GroupCreateUseCase,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val appConfig: AppConfigRepository,
    private val valueFormatter: IValueFormatter,
    private val deviceApiProvider: IDeviceApiProvider
) : CoreViewModel() {

    private val groupData = MutableStateFlow<UserGroupWithMembers?>(null)
    private val _uiState =
        MutableStateFlow<Resource<GroupSettingsScreenState>>(Resource.NotInitialized)
    val uiState: StateFlow<Resource<GroupSettingsScreenState>> = _uiState
    val groupNameState = uiState.map { it.data?.name }

    private val _eventChannel = Channel<GroupSettingsScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        groupLocalDataSource.getGroupWithMembers(groupId)
            .filterNotNull()
            .mapToPayload { group ->
                groupData.update { group }
                val planConfig = group.group.planConfig(remoteConfigRepo.getConfigOnce())
                val currentMember = group.currentUserMember
                val isOwner = group.isCurrentUserOwner

                GroupSettingsScreenState(
                    id = groupId,
                    canEdit = isOwner,
                    hasDevFeatures = appConfig.isInAppDevMode,
                    isMemberVerificationEnabled = group.data.data?.enforceMemberAuthentication != null,
                    isMemberVerificationOn = group.data.data?.enforceMemberAuthentication == true,
                    loadingAction = false,
                    name = group.name,
                    image = group.image,
                    planName = planConfig.adjustedPlanName,
                    featureTableState = constructFeatureTable(
                        planConfig = planConfig,
                        group = group.group,
                        config = remoteConfigRepo.getConfigOnce()
                    ),
                    alertDialog = _uiState.value.data?.alertDialog,
                    memberId = currentMember?.id,
                    locationId = group.currentDeviceLocation?.id,
                    createdAtDateFormatted = if (isOwner) {
                        formatInfoDate(group.group.createdAt)
                    } else {
                        null
                    },
                    memberSinceDateFormatted = if (!isOwner) {
                        formatInfoDate(currentMember?.createdAt)
                    } else {
                        null
                    }
                )
            }
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
            .launchIn(coroutineScope)
    }

    fun onEvent(event: GroupSettingsUiEvent) {
        when (event) {
            GroupSettingsUiEvent.BlockGroup -> updateDialog(MainAlertDialog.BlockGroup)
            GroupSettingsUiEvent.DeleteGroup -> updateDialog(MainAlertDialog.DeleteGroup)
            GroupSettingsUiEvent.LeaveGroup -> updateDialog(MainAlertDialog.LeaveGroup)
            GroupSettingsUiEvent.ToggleMemberVerification -> {
                _uiState.value.data?.let { data ->
                    if (data.canEdit && data.isMemberVerificationEnabled) {
                        updateDialog(
                            ToggleMemberVerification(turnOn = !data.isMemberVerificationOn)
                        )
                    }
                }
            }

            is GroupSettingsUiEvent.DialogAction -> {
                updateDialog(null)
                when (event.action.dialog) {
                    is MainAlertDialog.DeleteGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            leaveOrDeleteGroup()
                        }
                    }

                    is MainAlertDialog.LeaveGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            leaveOrDeleteGroup()
                        }
                    }

                    is MainAlertDialog.BlockGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            blockGroup()
                        }
                    }

                    is ToggleMemberVerification -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            toggleMembershipVerification()
                        }
                    }

                    else -> {}
                }
            }

            is GroupSettingsUiEvent.OnCellItemClick -> {
                when (event.cellItem) {
                    is FeatureTableItemValue.MoreInfo -> {
                        showSnackbar(
                            data = AppSnackbarData(
                                payload = AppSnackbarPayload(
                                    title = event.cellItem.title,
                                    text = event.cellItem.content,
                                    hasCloseButton = true
                                ),
                                duration = SnackbarDuration.Long
                            )
                        )
                    }

                    else -> {}
                }
            }

            is GroupSettingsUiEvent.Copy -> deviceApiProvider.copyToClipboard(event.content)
        }
    }

    private fun toggleMembershipVerification() {
        val group = groupData.value?.group?.takeIf { group ->
            group.isCurrentUserOwner && group.data.data != null
        } ?: return

        val newData = group.data.mapSuccess {
            it.copy(enforceMemberAuthentication = !it.enforceMemberAuthentication)
        }
        val newGroup = group.copy(data = newData)

        createGroup.updateGroup(newGroup)
            .mapToPayload { it }
            .onEach { data ->
                _uiState.update {
                    it.mapReady { it.copy(loadingAction = data is Payload.Loading) }
                }
                when (data) {
                    is Payload.Error -> {
                        showSnackbar(data.exception.errorString())
                    }

                    Payload.Loading -> {}
                    is Payload.Success -> {}
                }
            }
            .launchIn(coroutineScope)
    }

    private fun updateDialog(dialog: MainAlertDialog?) {
        if (groupPreRefresh.isProcessingAny() && dialog != null) {
            showSnackbar(DataStillLoadingException("Group not loaded").errorString())
        } else {
            _uiState.update { it.mapReady { it.copy(alertDialog = dialog) } }
        }
    }

    private fun leaveOrDeleteGroup() {
        processGroupRemoval { data ->
            if (data.canEdit) {
                groupRemove.deleteGroup(groupId).mapToPayload { it }
            } else {
                groupRemove.leaveGroup(groupId).mapToPayload { it }
            }
        }
    }

    private fun blockGroup() {
        processGroupRemoval { groupRemove.blockGroup(groupId).mapToPayload { it } }
    }

    private fun processGroupRemoval(
        action: (GroupSettingsScreenState) -> Flow<Payload<Unit>>
    ) {
        val data = _uiState.value.data

        if (data == null) {
            flowOf(Payload.Error<Unit>(NotFoundException("Group not loaded")))
        } else if (groupPreRefresh.isProcessingAny()) {
            flowOf(Payload.Error<Unit>(DataStillLoadingException("Group not loaded")))
        } else {
            action(data)
        }
            .onEach { result ->
                _uiState.update {
                    it.mapReady { it.copy(loadingAction = result is Payload.Loading) }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupSettingsScreenEvent.GroupDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun constructFeatureTable(
        planConfig: PlanConfig,
        group: UserGroup,
        config: ConfigData
    ): FeatureTableState {
        val currentPlan = planConfig.copy(
            membersLimit = group.membersLimit(config),
            devicesLimit = group.devicesLimit(config),
            placesLimit = group.placesLimit(config) ?: -1,
            privacyZonesLimit = group.privacyZonesLimit(config) ?: -1
        )
        return FeatureTableState.constructFeaturesTable(
            isGroupPlanOnly = true,
            plans = listOf(
                PlanInfoState(
                    productId = "",
                    name = currentPlan.adjustedPlanName,
                    config = currentPlan,
                    isCurrentPlan = true,
                    hasSpecialOffer = false,
                    isSelected = true,
                    isExpanded = true,
                    billingPeriods = emptyList()
                )
            )
        )
    }

    private fun formatInfoDate(instant: Instant?): String? {
        val dateTimeUTC = instant?.toLocalDateTime(TimeZone.UTC) ?: return null
        return valueFormatter.formatBestPattern(
            timeUTC = dateTimeUTC,
            pattern = "MMMMdyyyy"
        ).takeIf { it.isNotBlank() }
    }
}