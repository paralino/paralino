package app.paralino.core.presentation.groups

import app.paralino.core.config
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: String,
    private val placesRepo: PlacesRepository,
    private val sharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val groupRemove: GroupRemoveUseCase,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupRefresh: GroupRefreshUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<GroupDetailScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<GroupDetailScreenState>> = _uiState

    private val _eventChannel = Channel<GroupDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    init {
        combine(
            locationStatusProvider.status,
            connectionStatus.status,
            groupRefresh.getGroupWithMembers(groupId),
            placesRepo.hasAnyPlace(),
            sharingManager.sharingContractForGroup(groupId)
        ) { locationStatus, network, group, hasPlaces, sharingStatus ->

            val members = group.members
                .sortedBy { it.member.name }
                .sortedBy { it.member.userId != config.userId }
                .map { member ->
                    val canEdit = group.isCurrentUserOwner || member.member.userId == config.userId

                    val memberState = GroupMemberState(
                        member = member.member,
                        isCurrentUser = member.member.userId == config.userId
                    ).copy(
                        actionItem = MemberAction.DELETE_MEMBER.takeIf {
                            group.isCurrentUserOwner && member.member.userId != group.ownerUserId
                        }
                    )
                    if (member.locations.size <= 1) {
                        GroupMemberItemStateWithOneLocation(
                            memberState = memberState,
                            location = member.locations.firstOrNull()?.let {
                                GroupMemberItemLocationState(
                                    memberLocation = it,
                                    // don't show device name if there is only one location
                                    name = null,
                                    image = member.member.image,
                                    canEdit = false,
                                    currentDevice = it.deviceId == config.deviceId,
                                    showSpeedAndElevation = false
                                )
                            }
                        )
                    } else {
                        GroupMemberItemStateWithLocations(
                            memberState = memberState,
                            locations = member.locations.map {
                                GroupMemberItemLocationState(
                                    memberLocation = it,
                                    image = member.member.image,
                                    canEdit = canEdit,
                                    currentDevice = it.deviceId == config.deviceId,
                                    showSpeedAndElevation = false
                                )
                            }
                        )
                    }
                }

            val errors = buildList {
                sharingStatus?.let {
                    if(!network.isOnline)
                        add(ConditionErrorType.NETWORK)
                    addAll(locationStatus.getPossibleErrors(forBackground = it.isLongTerm))
                }

                if (hasPlaces && !locationStatus.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
                }
            }

            GroupDetailScreenState(
                id = groupId,
                canEdit = group.isCurrentUserOwner,
                loadingAction = false,
                name = group.name,
                image = group.image,
                sharingStatus = sharingStatus,
                members = members,
                memberLimit = group.group.planType.membersLimit,
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


    fun leaveOrDeleteGroup() {
        val data = _uiState.value.data

        if (data == null) {
            coroutineScope.launch {
                _eventChannel.send(GroupDetailScreenEvent.Error(NotFoundException("Group not loaded")))
            }
            flowOf(Payload.Error<Unit>(NotFoundException("Group not loaded")))
        } else if (data.canEdit) {
            groupRemove.deleteGroup(groupId).mapToPayload { it }
        } else {
            groupRemove.leaveGroup(groupId).mapToPayload { it }
        }
            .onEach { result ->
                _uiState.update { it.mapSuccess { it.copy(loadingAction = result is Payload.Loading) } }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(GroupDetailScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _eventChannel.send(GroupDetailScreenEvent.GroupDeleted)
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
            if(state.members.size >= state.memberLimit)
                _eventChannel.send(GroupDetailScreenEvent.GroupMembersLimitReached(state.memberLimit))
            else
                _eventChannel.send(GroupDetailScreenEvent.GroupMemberAdd)
        }
    }

    fun stopSharing() {
        coroutineScope.launch {
            sharingManager.stopSharing(groupId)
        }
    }

}