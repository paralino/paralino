package app.paralino.core.presentation.invite.input

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupMemberAddUseCase
import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.utils.exceptions.ConflictException
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemberInviteViewModel(
    val groupId: String?,
    code: String?,
    val isOnboarding: Boolean,
    private val codeRepo: PersonalCodesRepository,
    private val groupRepo: UserGroupLocalDataSource,
    private val groupMemberAdd: GroupMemberAddUseCase,
    private val userRepo: UserRepository,
    private val premiumStatusManager: PremiumStatusManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MemberInviteScreenState>(
        MemberInviteScreenState()
    )
    val uiState: StateFlow<MemberInviteScreenState> = _uiState

    private val currentGroup = MutableStateFlow<GroupItemState?>(null)
    val currentGroupId: String? get() = currentGroup.value?.id?.id ?: groupId

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

        currentGroup
            .filterNotNull()
            .map { it.id }
            .distinctUntilChanged()
            .onEach { group ->
                _uiState.update { state ->
                    state.copy(
                        premiumStatus = premiumStatusManager.checkMemberCountLimits(groupId = group.id)
                    )
                }
            }
            .flatMapLatest {
                groupRepo.getGroupWithMembers(groupId = it.id)
                    .filterNotNull()
            }
            .onEach { group ->
                _uiState.update {
                    it.copy(
                        members = group.members.map { it.member }
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: MemberInviteScreenUiEvent) {
        when (event) {
            is MemberInviteScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.ServiceMaxLimit,
                    is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update { it.copy(alertDialog = null) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun addOrLoadMember() {
        val currentState = _uiState.value

        if (currentState.loadedState == null) {
            if (currentState.code.isBlank()) {
                // load user
                _uiState.update {
                    it.copy(
                        hasInputError = true,
                        actionLoading = false
                    )
                }
                showSnackbar(MR.strings.invite_connect_alert_wrongCode.localizedString())
            } else {
                loadMemberData(currentState.code)
            }
        } else {
            // add member
            val groupId = currentGroup.value?.id?.id
            if (currentState.code.isBlank()) {
                _uiState.update {
                    it.copy(
                        hasInputError = true,
                        actionLoading = false,
                        loadedState = null
                    )
                }
            } else if (groupId.isNullOrBlank()) {
                _uiState.update {
                    currentState.copy(
                        loadedState = currentState.loadedState.copy(
                            canChangeGroup = true
                        )
                    )
                }
                coroutineScope.launch {
                    _eventChannel.send(MemberInviteScreenEvent.ShowGroupPicker(getGroupPickerState()))
                }
            } else {
                addMemberToGroup(
                    groupId = groupId,
                    state = currentState.loadedState
                )
            }
        }
    }

    private fun loadMemberData(code: String) {
        codeRepo.getUserInfoFromCode(code)
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update {
                            it.copy(
                                code = code,
                                actionLoading = true,
                                hasInputError = false,
                                loadedState = null
                            )
                        }
                    }

                    is Payload.Error -> {
                        _uiState.update {
                            it.copy(
                                code = code,
                                actionLoading = false,
                                hasInputError = false,
                                loadedState = null
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
                            it.copy(
                                actionLoading = false,
                                hasInputError = false,
                                group = currentGroup.value,
                                loadedState = MemberInviteScreenLoadedState(
                                    codeReadData = result.data,
                                    canChangeGroup = groupId.isNullOrBlank()
                                )
                            )
                        }
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun addMemberToGroup(groupId: String, state: MemberInviteScreenLoadedState) {
        when (val premiumStatus = uiState.value.premiumStatus) {
            is PremiumStatusLimitState.PlanLimitReached -> {
                coroutineScope.launch {
                    _eventChannel.send(MemberInviteScreenEvent.OpenPaywall(BillingScreenSourceType.UPGRADE))
                }
            }

            is PremiumStatusLimitState.ServiceLimitReached -> {
                _uiState.update {
                    it.copy(
                        alertDialog = MainAlertDialog.ServiceMaxLimit(
                            content = MR.strings.plan_limit_members_alert_info.localizedString(),
                            limit = premiumStatus.payload.limit ?: -1
                        )
                    )
                }
            }

            is PremiumStatusLimitState.WithinLimits -> {
                groupMemberAdd.addMemberByAccessCode(groupId = groupId, code = state.codeReadData)
                    .mapToPayload { it }
                    .onEach { result ->
                        when (result) {
                            Payload.Loading -> {
                                _uiState.update {
                                    it.copy(actionLoading = true)
                                }
                            }

                            is Payload.Error -> {
                                if (result.exception.asRemoteException() is ConflictException) {
                                    showSnackbar(MR.strings.invite_connect_already_member_message.localizedString())
                                    delay(2000)
                                    _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                                } else {
                                    _uiState.update {
                                        it.copy(actionLoading = false)
                                    }
                                    showSnackbar(result.exception.errorString())
                                }
                            }

                            is Payload.Success -> {
                                _uiState.update { it.copy(actionLoading = false) }

                                if (isOnboarding) {
                                    _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                                } else {
                                    if (userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium) {
                                        _eventChannel.send(MemberInviteScreenEvent.MemberAdded)
                                    } else {
                                        _eventChannel.send(
                                            MemberInviteScreenEvent.OpenPaywall(
                                                BillingScreenSourceType.FREE_PLAN_NEW_GROUP_MEMBER
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .launchIn(coroutineScope)
            }

            null -> {}
        }
    }

    suspend fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = true,
            showPersonalSpace = false,
            userPlan = null,
            personalSpace = null,
            createdGroups = getAllUserCreatedGroups(),
            joinedGroups = emptyList()
        )
    }

    private suspend fun getAllUserCreatedGroups(): List<GroupItemState> {
        return groupRepo.getAllGroupsWithMetadata()
            .firstOrNull()
            ?.filter { it.group.isCurrentUserOwner }
            ?.map { GroupItemState(it) }
            ?: emptyList()
    }

    fun selectGroup(group: GroupItemState) {
        currentGroup.value = group
        _uiState.update {
            if (it.loadedState != null) it.copy(group = group)
            else it
        }
    }

    fun codeChanged(code: String) {
        _uiState.update {
            it.copy(
                code = code
                    .trim()
                    .let { DeeplinkType.ParalinoID.getParalinoIDCode(it) }
                    .uppercase(),
                hasInputError = if (it.hasInputError) code.isBlank() else false,
                loadedState = null
            )
        }
    }
}