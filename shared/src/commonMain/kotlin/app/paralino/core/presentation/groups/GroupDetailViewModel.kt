package app.paralino.core.presentation.groups

import app.paralino.core.BuildKonfig
import app.paralino.core.config
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    val groupId: String,
    private val userRepo: UserRepository,
    private val appConfigRepo: AppConfigRepository,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val sharingManager: LocationSharingManager,
    private val placeActivityRepo: PlaceActivityDataSource,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupRefresh: GroupPreRefreshUseCase,
    private val valueFormatter: IValueFormatter,
    private val deviceApiProvider: IDeviceApiProvider
) : CoreViewModel() {

    private val groupData = MutableStateFlow<UserGroupWithMembers?>(null)
    private val _uiState = MutableStateFlow<Payload<GroupDetailScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<GroupDetailScreenState>> = _uiState

    private val _eventChannel = Channel<GroupDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            locationStatusProvider.status,
            connectionStatus.status,
            groupRefresh.getAndRefreshGroupWithMembers(groupId),
            sharingManager.sharingContractForGroup(groupId),
            userRepo.getBlocklistLocally()
        ) { locationStatus, network, group, sharingStatus, blocklist ->
            groupData.update { group }
            val members = group.members
                .sortedBy { it.member.name }
                .sortedBy { it.member.userId != config.userId }
                .map { member ->
                    val canEdit = group.isCurrentUserOwner || member.member.userId == config.userId

                    val memberState = GroupMemberState(
                        member = member.member,
                        isCurrentUser = member.member.userId == config.userId,
                        canHaveExpandButton = true
                    )
                    GroupMemberItemStateWithLocations(
                        memberState = memberState,
                        locations = member.locations.map {
                            GroupMemberItemLocationState(
                                memberLocation = it,
                                image = member.member.image,
                                canEdit = canEdit,
                                currentDevice = it.deviceId == config.deviceId,
                                showSpeedAndElevation = false,
                                formatter = valueFormatter,
                                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
                            )
                        },
                        actions = MemberActionsState(
                            deleteMember = takeIf { group.isCurrentUserOwner && member.member.userId != group.ownerUserId }?.let {
                                MemberActionItemState(MemberActionType.DELETE_MEMBER)
                            },
                            blockUser = takeIf { member.member.userId != config.userId }
                                ?.takeIf { blocklist.none { it.resourceId == member.member.userId } }
                                ?.let {
                                    MemberActionItemState(MemberActionType.BLOCK_USER)
                                },
                            unblockUser = takeIf { member.member.userId != config.userId }
                                ?.takeIf { blocklist.any { it.resourceId == member.member.userId } }
                                ?.let {
                                    MemberActionItemState(MemberActionType.UNBLOCK_USER)
                                },
                            securityAction = BuildKonfig.hasPreviewFeatures
                        ),
                        allowShowingDevices = true,
                        lastLocationPing = valueFormatter.getLastLocationPing(member),
                        lastStatusPing = valueFormatter.getLastStatusPing(member)
                    )
                }

            val errors = buildList {
                sharingStatus?.let {
                    if (!network.isOnline)
                        add(ConditionErrorType.NETWORK)
                    addAll(locationStatus.getPossibleErrors(forBackground = it.isLongTerm))
                }

                if (!locationStatus.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
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
                    approximateLocation = it.approximateLocationInMeters?.let {
                        valueFormatter.getDistanceSmall(it.toDouble())
                    }
                )
            }

            val membersLimit = group.group.membersLimitOverride?.toInt()
                ?: appConfigRepo.configState.value.planConfigForPlanType(group.group.planType).membersLimit
            GroupDetailScreenState(
                id = groupId,
                canEdit = group.isCurrentUserOwner,
                loadingAction = false,
                name = group.name,
                image = group.image,
                sharingStatus = sharingState,
                members = members,
                membersLimit = membersLimit,
                canShowMembersLimit = group.isCurrentUserOwner,
                errors = errors
            )
        }
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { result }
                if (result is Payload.Error) {
                    if (result.exception.asRemoteException() is NotFoundException)
                        _eventChannel.send(GroupDetailScreenEvent.GroupNotFound)
                    else
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                }
            }
            .launchIn(coroutineScope)
    }

    fun blockUser(userId: String) {
        val member = groupData.value?.memberByUserId(userId = userId) ?: return
        groupMemberRemove.blockUser(member)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.UserBlockActionCompleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun unblockUser(userId: String) {
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
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.UserBlockActionCompleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun removeMember(memberUserId: String) {
        groupMemberRemove.removeMember(groupId, memberUserId)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.GroupMemberDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun removeMemberLocation(memberId: String, locationId: String) {
        groupMemberRemove.deleteLocation(memberId, locationId)
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.MemberLocationDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun tryAddMember() {
        val state = _uiState.value.data ?: return
        coroutineScope.launch {
            if (state.members.size >= state.membersLimit)
                _eventChannel.send(GroupDetailScreenEvent.GroupMembersLimitReached(state.membersLimit))
            else
                _eventChannel.send(GroupDetailScreenEvent.GroupMemberAdd)
        }
    }

    fun stopSharing() {
        coroutineScope.launch {
            sharingManager.stopSharing(groupId)
            placeActivityRepo.clearForGroup(groupId = groupId)
            try {
                deviceStatusProcessor.pushDeviceStatus(groupId = groupId)
            } catch (e: Exception) {
                logg.w("Error while updating user status", e)
            }
        }
    }

    fun copyToClipboard(data: String) {
        deviceApiProvider.copyToClipboard(data)
    }
}