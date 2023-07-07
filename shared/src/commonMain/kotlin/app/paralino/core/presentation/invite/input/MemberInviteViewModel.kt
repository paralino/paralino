package app.paralino.core.presentation.invite.input

import app.paralino.core.domain.group.GroupMemberAddUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemberInviteViewModel(
    val groupId: String?,
    code: String?,
    private val codeRepo: PersonalCodesRepository,
    private val groupRepo: GroupRepository,
    private val groupMemberAdd: GroupMemberAddUseCase,
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow<MemberInviteScreenState>(MemberInviteScreenState.Empty())
    val uiState: Flow<MemberInviteScreenState> = _uiState

    private val groups = MutableStateFlow<List<GroupItemState>>(emptyList())
    private val currentGroup = MutableStateFlow<GroupItemState?>(null)

    private val _eventChannel = Channel<MemberInviteScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        code?.let { loadMemberData(code = it) }
        groupId?.let {
            coroutineScope.launch {
                groupRepo.getGroupWithoutMembersLocally(it)
                    .firstOrNull()
                    ?.let { GroupItemState(it) }
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
                    coroutineScope.launch {
                        _eventChannel.send(MemberInviteScreenEvent.ErrorWrongCode)
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
        codeRepo.getUserInfoFromCode(code)
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
                        if(result.exception.asRemoteException() is NotFoundException)
                            _eventChannel.send(MemberInviteScreenEvent.ErrorWrongCode)
                        else _eventChannel.send(MemberInviteScreenEvent.Error(result.exception))
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
        groupMemberAdd.addMemberByAccessCode(groupId, state.codeReadData)
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

    suspend fun getAllGroups(): List<GroupItemState> {
        return groupRepo.getAllGroupsLocally()
            .firstOrNull()
            ?.map { GroupItemState(it) }
            ?.also { groups.value = it }
            ?: emptyList()
    }

    fun selectGroup(group: GroupItemState) {
        currentGroup.value = group
        _uiState.update {
            if (it is MemberInviteScreenState.Loaded) it.copy(group = group)
            else it
        }
    }

    fun codeChanged(code: String) {
        _uiState.update {
            MemberInviteScreenState.Empty(
                code = code.trim().uppercase(),
                hasInputError = if (it.hasInputError) code.isBlank() else false,
                actionLoading = it.actionLoading
            )
        }
    }

}