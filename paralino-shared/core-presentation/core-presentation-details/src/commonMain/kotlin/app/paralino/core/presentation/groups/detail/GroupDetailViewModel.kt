package app.paralino.core.presentation.groups.detail

import app.paralino.core.MR
import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.GroupMemberItemEvent
import app.paralino.core.presentation.common.GroupMemberItemLocationState
import app.paralino.core.presentation.common.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.common.GroupMemberState
import app.paralino.core.presentation.common.MemberActionItemState
import app.paralino.core.presentation.common.MemberActionType
import app.paralino.core.presentation.common.MemberActionsState
import app.paralino.core.presentation.common.MemberLocationVisibilityType
import app.paralino.core.presentation.groups.detail.GroupDetailScreenEvent.OpenRawMemberData
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.onAction
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.MandatoryBackgroundLocationSharingConditionType
import app.paralino.core.utils.MandatoryForegroundLocationSharingConditionType
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    val groupId: String,
    private val userRepo: UserRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val sharingManager: LocationSharingManager,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupRefresh: GroupPreRefreshUseCase,
    private val groupLocal: UserGroupLocalDataSource,
    private val premiumStatusManager: PremiumStatusManager,
    private val valueFormatter: IValueFormatter,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val appConfig: AppConfigRepository,
    private val appRatingManager: AppRatingManager
) : CoreViewModel() {

    private val groupData = MutableStateFlow<UserGroupWithMembers?>(null)
    private val groupWithMetadata = MutableStateFlow<UserGroupWithMetadata?>(null)
    private val _uiState = MutableStateFlow<Payload<GroupDetailScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<GroupDetailScreenState>> = _uiState

    private val _eventChannel = Channel<GroupDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.GROUP_DETAIL)

        combine(
            deviceApiProvider.deviceStatus,
            groupRefresh.getAndRefreshGroupWithMembers(groupId),
            sharingManager.sharingContractForGroup(groupId.asGroupOrSpaceId()),
            userRepo.getBlocklistLocally()
        ) { deviceStatus, group, sharingStatus, blocklist ->
            groupData.update { group }
            val members = group.members
                .sortedBy { it.member.name }
                .sortedBy { it.member.userId != appPrefs.userId }
                .map { member ->
                    val canEdit =
                        group.isCurrentUserOwner || member.member.userId == appPrefs.userId

                    val memberState = GroupMemberState(
                        member = member.member,
                        isCurrentUser = member.member.userId == appPrefs.userId,
                        canHaveExpandButton = true
                    )
                    GroupMemberItemStateWithLocations(
                        memberState = memberState,
                        locations = member.locations.map {
                            GroupMemberItemLocationState(
                                memberLocation = it,
                                image = member.member.image,
                                canEdit = canEdit,
                                currentDevice = it.deviceId == appPrefs.deviceId,
                                showSpeedAndElevation = false,
                                formatter = valueFormatter,
                                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
                            )
                        },
                        actions = MemberActionsState(
                            deleteMember = takeIf { group.isCurrentUserOwner && member.member.userId != group.ownerUserId }?.let {
                                MemberActionItemState(MemberActionType.DELETE_MEMBER)
                            },
                            blockUser = takeIf { member.member.userId != appPrefs.userId }
                                ?.takeIf { blocklist.none { it.resourceId == member.member.userId } }
                                ?.let {
                                    MemberActionItemState(MemberActionType.BLOCK_USER)
                                },
                            unblockUser = takeIf { member.member.userId != appPrefs.userId }
                                ?.takeIf { blocklist.any { it.resourceId == member.member.userId } }
                                ?.let {
                                    MemberActionItemState(MemberActionType.UNBLOCK_USER)
                                },
                            securityAction = appConfig.isInAppDevMode
                        ),
                        allowShowingDevices = true,
                        lastLocationPing = valueFormatter.getLastLocationPing(member),
                        lastStatusPing = valueFormatter.getLastStatusPing(member)
                    )
                }

            val errors = buildList {
                sharingStatus?.let { status ->
                    if (!deviceStatus.networkStatus.isOnline) {
                        add(ConditionErrorType.Network)
                    }

                    addAll(
                        deviceStatus.locationStatus.getPossibleErrors()
                            .filter {
                                if (status.isLongTerm) {
                                    it is MandatoryBackgroundLocationSharingConditionType
                                } else {
                                    it is MandatoryForegroundLocationSharingConditionType
                                }
                            }
                    )
                }

                if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PermissionNotifications)
                }
            }

            val sharingState = sharingStatus?.let {
                LocationSharingState(
                    endTime = it.endTimeUTC?.let {
                        valueFormatter.fromUTCtoMapSharingTime(
                            timeUTC = it,
                            splitTimeToOneLine = false
                        )
                    },
                    shareBattery = it.shareBattery,
                    shareSpeed = it.shareSpeed,
                    shareElevation = it.shareElevation,
                    shareNetwork = it.shareNetwork,
                    shareDeviceStatus = it.shareDeviceStatus,
                    saveToHistory = it.saveToHistory,
                    approximateLocation = it.approximateLocationInMeters?.let {
                        valueFormatter.getDistanceSmall(
                            meters = it.toDouble(),
                            distanceType = DistanceType.DISTANCE
                        )
                    }
                )
            }

            val membersLimit = group.group.membersLimit(remoteConfigRepo.getConfigOnce())
            val currentState = _uiState.value
            GroupDetailScreenState(
                groupId = groupId,
                canEdit = group.isCurrentUserOwner,
                loadingAction = false,
                name = group.name,
                image = group.image,
                sharingStatus = sharingState,
                members = members,
                focusedMemberId = currentState.data?.focusedMemberId,
                membersLimit = membersLimit,
                canShowMembersLimit = group.isCurrentUserOwner || membersLimit < members.size,
                shouldShowPlanBanner = group.isCurrentUserOwner && !group.group.isGroupPremium(
                    remoteConfigRepo.getConfigOnce()
                ),
                errors = errors,
                arePlacesOverLimit = groupWithMetadata.value?.arePlacesOverLimits(remoteConfigRepo.getConfigOnce()),
                isGroupOverLimit = groupWithMetadata.value?.isGroupOverLimits(remoteConfigRepo.getConfigOnce()),
                alertDialog = currentState.data?.alertDialog
            )
        }
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { result }
                if (result is Payload.Error) {
                    if (result.exception.asRemoteException() is NotFoundException) {
                        showSnackbar(MR.strings.group_load_error.localizedString())
                    } else {
                        showSnackbar(result.exception.errorString())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        groupLocal.getGroupWithMetadata(groupId)
            .onEach { group -> groupWithMetadata.update { group } }
            .filter { it.isGroupOverLimits(remoteConfigRepo.getConfigOnce()) }
            .distinctUntilChanged()
            .onEach { group ->
                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            arePlacesOverLimit = group.arePlacesOverLimits(remoteConfigRepo.getConfigOnce()),
                            isGroupOverLimit = group.isGroupOverLimits(remoteConfigRepo.getConfigOnce()),
                        )
                    }
                }
                if (group.group.isCurrentUserOwner) {
                    showSnackbarForResult(
                        data = AppSnackbarData(
                            text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                            duration = SnackbarDuration.Forever,
                            actionText = MR.strings.plans_banner_settings_action_small.localizedString()
                        )
                    )?.result?.onAction {
                        _eventChannel.trySend(GroupDetailScreenEvent.OpenPaywall)
                    }
                } else {
                    showSnackbar(
                        text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                        duration = SnackbarDuration.Forever
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: GroupDetailScreenUiEvent) {
        when (event) {
            is GroupDetailScreenUiEvent.DialogAction -> {
                when (val dialog = event.action.dialog) {
                    is MainAlertDialog.ServiceMaxLimit,
                    is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                    }

                    is MainAlertDialog.BlockUser -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        if (event.action.isOk) {
                            blockUser(dialog.userId)
                        }
                    }

                    is MainAlertDialog.UnblockGroupOrUser -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        if (event.action.isOk) {
                            unblockUser(dialog.id)
                        }
                    }

                    is MainAlertDialog.DeleteGroupMember -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        if (event.action.isOk) {
                            removeMember(dialog.memberUserId)
                        }
                    }

                    is MainAlertDialog.DeleteMemberLocation -> {
                        if (event.action.isOk) {
                            coroutineScope.launch {
                                _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                                delay(200)
                                _uiState.update {
                                    it.mapSuccess {
                                        it.copy(
                                            alertDialog = MainAlertDialog.DeleteMemberLocationConfirm(
                                                memberId = dialog.memberId,
                                                locationId = dialog.locationId,
                                                isUserOwnDevice = dialog.isUserOwnDevice,
                                                memberName = dialog.memberName
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        }
                    }

                    is MainAlertDialog.DeleteMemberLocationConfirm -> {
                        _uiState.update { it.mapSuccess { it.copy(alertDialog = null) } }
                        if (event.action.isOk) {
                            removeMemberLocation(
                                memberId = dialog.memberId,
                                locationId = dialog.locationId
                            )
                        }
                    }

                    else -> {}
                }
            }

            GroupDetailScreenUiEvent.OnWholeContentClick -> {
                _uiState.update {
                    it.mapSuccess {
                        it.copy(focusedMemberId = null)
                    }
                }
            }

            is GroupDetailScreenUiEvent.OnMemberItemEvent -> {
                when (event.event) {
                    is GroupMemberItemEvent.onActionClick -> {
                        when (event.event.action.type) {
                            MemberActionType.DELETE_MEMBER -> {
                                _uiState.update {
                                    it.mapSuccess {
                                        it.copy(
                                            alertDialog = MainAlertDialog.DeleteGroupMember(
                                                memberUserId = event.event.item.memberState.userId,
                                                memberName = event.event.item.memberState.name
                                            )
                                        )
                                    }
                                }
                            }

                            MemberActionType.BLOCK_USER -> {
                                _uiState.update {
                                    it.mapSuccess {
                                        it.copy(
                                            alertDialog = MainAlertDialog.BlockUser(
                                                userId = event.event.item.memberState.userId,
                                            )
                                        )
                                    }
                                }
                            }

                            MemberActionType.UNBLOCK_USER -> {
                                _uiState.update {
                                    it.mapSuccess {
                                        it.copy(
                                            alertDialog = MainAlertDialog.UnblockGroupOrUser(
                                                id = event.event.item.memberState.userId,
                                                type = BlocklistType.USER
                                            )
                                        )
                                    }
                                }
                            }

                            MemberActionType.DELETE_LOCATION -> {}
                        }
                    }

                    is GroupMemberItemEvent.onCopyCode -> {
                        copyToClipboard(event.event.code)
                    }

                    is GroupMemberItemEvent.onDeleteDeviceClick -> {
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    alertDialog = MainAlertDialog.DeleteMemberLocation(
                                        memberId = event.event.item.memberState.memberId,
                                        locationId = event.event.device.locationId,
                                        isUserOwnDevice = event.event.item.memberState.isCurrentUser,
                                        memberName = event.event.item.memberState.name
                                    )
                                )
                            }
                        }
                    }

                    is GroupMemberItemEvent.onOpenRawData -> {
                        coroutineScope.launch {
                            _eventChannel.send(
                                OpenRawMemberData(
                                    memberId = event.event.item.memberState.memberId
                                )
                            )
                        }
                    }

                    is GroupMemberItemEvent.onItemClick -> {
                        _uiState.update {
                            it.mapSuccess {
                                it.copy(
                                    focusedMemberId = if (it.focusedMemberId.isNullOrBlank()) {
                                        event.event.item.memberState.memberId
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }

                    is GroupMemberItemEvent.onDirectionsRequest,
                    is GroupMemberItemEvent.onLocationRequest,
                    is GroupMemberItemEvent.onDeviceClick,
                    is GroupMemberItemEvent.onOpenLocationHistory,
                    is GroupMemberItemEvent.onShowLocation -> {
                    }
                }
            }

            GroupDetailScreenUiEvent.OnAddNewMember -> tryAddMember()
            GroupDetailScreenUiEvent.StopSharing -> stopSharing()
            GroupDetailScreenUiEvent.OpenMap -> {
                appPrefs.selectedGroupId = groupId
                appPrefs.isPersonalSpaceSelected = false
                _eventChannel.trySend(GroupDetailScreenEvent.OpenMap)
            }
        }
    }

    private fun blockUser(userId: String) {
        val member = groupData.value?.memberByUserId(userId = userId)?.member ?: return
        groupMemberRemove.blockUser(member)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.UserBlockActionCompleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun unblockUser(userId: String) {
        flow {
            userRepo.removeUserFromBlocklist(userId = userId)
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.UserBlockActionCompleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun removeMember(memberUserId: String) {
        groupMemberRemove.removeMember(groupId = groupId, memberUserId = memberUserId)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.GroupMemberDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun removeMemberLocation(memberId: String, locationId: String) {
        groupMemberRemove.deleteLocation(memberId = memberId, locationId = locationId)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.MemberLocationDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun tryAddMember() {
        val state = _uiState.value.data ?: return
        coroutineScope.launch {
            when (
                val premiumStatus = premiumStatusManager.checkMemberCountLimits(groupId = state.groupId)
            ) {
                is PremiumStatusLimitState.PlanLimitReached -> {
                    _eventChannel.send(GroupDetailScreenEvent.OpenPaywall)
                }

                is PremiumStatusLimitState.ServiceLimitReached -> {
                    _uiState.update {
                        it.mapSuccess {
                            it.copy(
                                alertDialog = MainAlertDialog.ServiceMaxLimit(
                                    content = MR.strings.plan_limit_members_alert_info.localizedString(),
                                    limit = premiumStatus.payload.limit ?: -1
                                )
                            )
                        }
                    }
                }

                is PremiumStatusLimitState.WithinLimits -> {
                    _eventChannel.send(GroupDetailScreenEvent.GroupMemberAdd)
                }

                null -> {}
            }
        }
    }

    private fun stopSharing() {
        coroutineScope.launch {
            sharingManager.stopSharing(groupId = groupId.asGroupOrSpaceId(), deleteContract = false)
            placeActivityRepo.clearForGroup(groupId = groupId.asGroupOrSpaceId())
            try {
                deviceStatusProcessor.pushDeviceStatus(groupId = groupId.asGroupOrSpaceId())
            } catch (e: Exception) {
                logg.w("Error while updating user status", e)
            }
        }
    }

    private fun copyToClipboard(data: String) {
        deviceApiProvider.copyToClipboard(data)
    }
}