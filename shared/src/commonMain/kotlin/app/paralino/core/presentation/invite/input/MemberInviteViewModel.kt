package app.paralino.core.presentation.invite.input

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupMemberAddUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemberInviteViewModel(
    val groupId: String?,
    code: String?,
    private val codeRepo: PersonalCodesRepository,
    private val groupRepo: UserGroupLocalDataSource,
    private val groupMemberAdd: GroupMemberAddUseCase,
    private val userRepo: UserRepository
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
                groupRepo.getGroupWithMetadata(it)
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
                    showSnackbar(MR.strings.invite_connect_alert_wrongCode.localizedString())
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
                    showSnackbar(MR.strings.invite_connect_alert_pickGroup.localizedString())
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
                        if (result.exception.asRemoteException() is NotFoundException) {
                            showSnackbar(MR.strings.invite_connect_alert_wrongCode.localizedString())
                        } else {
                            showSnackbar(result.exception.errorString())
                        }
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
                        if (result.exception.asRemoteException() is ConflictException) {
                            showSnackbar(MR.strings.invite_connect_already_member_message.localizedString())
                            delay(2000)
                            _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                        } else {
                            _uiState.update {
                                state.copy(actionLoading = false)
                            }
                            showSnackbar(result.exception.errorString())
                        }
                    }

                    is Payload.Success -> {
                        _uiState.update { state.copy(actionLoading = false) }
                        _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    suspend fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        return BottomSheetPickerType.GroupPickerBottomSheet(
            userPlan = userRepo.userPlans.value.highestValidPlanConfig,
            groups = getAllGroups()
        )
    }

    private suspend fun getAllGroups(): List<GroupItemState> {
        return groupRepo.getAllGroupsWithMetadata()
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