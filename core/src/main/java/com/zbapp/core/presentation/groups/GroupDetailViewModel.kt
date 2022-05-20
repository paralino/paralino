package com.zbapp.core.presentation.groups

import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.exceptions.NotFoundException
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepo: GroupRepository,
    private val sharingManager: LocationSharingManager,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<GroupDetailScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<GroupDetailScreenState>> = _uiState

    private val _eventChannel = Channel<GroupDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    init {
        combine(
            groupRepo.getGroupWithMembers(groupId),
            sharingManager.sharingContractForGroup(groupId)
        )
        { group, sharingStatus ->

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

            GroupDetailScreenState(
                id = groupId,
                canEdit = group.isCurrentUserOwner,
                loadingAction = false,
                name = group.name,
                image = group.image,
                sharingStatus = sharingStatus,
                members = members
            )
        }
            .mapToPayload { it }
            .onEach { result -> _uiState.update { result } }
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
            groupRepo.deleteGroup(groupId).mapToPayload { it }
        } else {
            groupRepo.leaveGroup(groupId).mapToPayload { it }
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
        groupRepo.removeMember(groupId, memberUserId)
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