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
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: String,
    private val placesRepo: PlacesRepository,
    private val sharingManager: LocationSharingManager,
    private val locationStatusProvider: ILocationStatusProvider,
    private val groupRemove: GroupRemoveUseCase,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupRefresh: GroupRefreshUseCase,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<GroupDetailScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<GroupDetailScreenState>> = _uiState

    private val _eventChannel = Channel<GroupDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    init {
        combine(
            locationStatusProvider.status,
            groupRefresh.getGroupWithMembers(groupId),
            placesRepo.hasAnyPlace(),
            sharingManager.sharingContractForGroup(groupId)
        )
        { locationStatus, group, hasPlaces, sharingStatus ->

            val members = group.members
                .sortedBy { it.name }
                .sortedBy { it.userId != config.userId }
                .map { member ->
                    GroupMemberItemState(member).copy(
                        actionItem = MemberAction.DELETE_MEMBER.takeIf {
                            group.isCurrentUserOwner && member.userId != group.ownerUserId
                        }
                    )
                }

            val errors = buildList {
                sharingStatus?.let {
                    addAll(locationStatus.getPossibleErrors(forBackground = it.isLongTerm))
                }

                if(hasPlaces && !locationStatus.isNotificationPermissionAllowed) {
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
                errors = errors
            )
        }
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { result }
                if(result is Payload.Error) {
                    if(result.exception.asRemoteException() is NotFoundException)
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

    fun stopSharing() {
        coroutineScope.launch {
            sharingManager.stopSharing(groupId)
        }
    }

}