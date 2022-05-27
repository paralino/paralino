package com.zbapp.core.presentation.invite.input

import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemberInviteViewModel(
    val groupId: String?,
    code: String?,
    private val inviteRepo: InviteRepository,
    private val groupRepo: GroupRepository,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState =
        MutableStateFlow<MemberInviteScreenState>(MemberInviteScreenState.Empty())
    val uiState: Flow<MemberInviteScreenState> = _uiState

    private val groups = MutableStateFlow<List<GroupItemData>>(emptyList())
    private val currentGroup = MutableStateFlow<GroupItemData?>(null)

    private val _eventChannel = Channel<MemberInviteScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        code?.let { loadMemberData(code = it) }
        groupId?.let {
            coroutineScope.launch {
                groupRepo.getGroupWithoutMembersLocally(it)
                    .firstOrNull()
                    ?.let { GroupItemData(it) }
                    ?.let { selectGroup(it) }
            }
        }
    }

    fun addOrLoadMember() {
        when (val currentState = _uiState.value) {
            is MemberInviteScreenState.Empty -> {
                // load user
                if (currentState.code.isBlank()) {
                    _uiState.update {
                        MemberInviteScreenState.Empty(
                            hasInputError = true,
                            actionLoading = false
                        )
                    }
                } else {
                    loadMemberData(currentState.code)
                }
            }
            is MemberInviteScreenState.Loaded -> {
                // add member
                val groupId = currentGroup.value?.id
                if (currentState.code.isBlank()) {
                    _uiState.update {
                        MemberInviteScreenState.Empty(
                            hasInputError = true,
                            actionLoading = false
                        )
                    }
                } else if (groupId.isNullOrBlank()) {
                    _uiState.update {
                        currentState.copy(canChangeGroup = true)
                    }
                    coroutineScope.launch {
                        _eventChannel.send(MemberInviteScreenEvent.ErrorMissingGroup)
                    }
                } else {
                    addMemberToGroup(groupId, currentState)
                }
            }
        }
    }

    private fun loadMemberData(code: String) {
        inviteRepo.getInviteByCode(code)
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update {
                            MemberInviteScreenState.Empty(
                                code = code,
                                actionLoading = true,
                                hasInputError = false
                            )
                        }
                    }
                    is Payload.Error -> {
                        _uiState.update {
                            MemberInviteScreenState.Empty(
                                code = code,
                                actionLoading = false,
                                hasInputError = false
                            )
                        }
                        _eventChannel.send(MemberInviteScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _uiState.update {
                            MemberInviteScreenState.Loaded(
                                codeReadData = result.data,
                                group = currentGroup.value,
                                actionLoading = false,
                                hasInputError = false,
                                canChangeGroup = groupId.isNullOrBlank()
                            )
                        }
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun addMemberToGroup(groupId: String, state: MemberInviteScreenState.Loaded) {
        groupRepo.addMemberByAccessCode(groupId, state.codeReadData)
            .mapToPayload { it }
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update {
                            state.copy(actionLoading = true)
                        }
                    }
                    is Payload.Error -> {
                        _uiState.update {
                            state.copy(actionLoading = false)
                        }
                        _eventChannel.send(MemberInviteScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _uiState.update { state.copy(actionLoading = false) }
                        _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    suspend fun getAllGroups(): List<GroupItemData> {
        return groupRepo.getAllGroupsLocally()
            .firstOrNull()
            ?.map { GroupItemData(it) }
            ?.also { groups.value = it }
            ?: emptyList()
    }

    fun selectGroup(group: GroupItemData) {
        currentGroup.value = group
        _uiState.update {
            if (it is MemberInviteScreenState.Loaded) it.copy(group = group)
            else it
        }
    }

    fun codeChanged(code: String) {
        _uiState.update {
            MemberInviteScreenState.Empty(
                code = code,
                hasInputError = if (it.hasInputError) code.isBlank() else false,
                actionLoading = it.actionLoading
            )
        }
    }

}