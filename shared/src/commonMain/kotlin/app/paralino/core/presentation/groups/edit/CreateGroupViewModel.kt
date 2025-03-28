package app.paralino.core.presentation.groups.edit

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupCreateUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupData
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    val groupId: String?,
    private val groupRepo: UserGroupLocalDataSource,
    private val userRepo: UserRepository,
    private val remoteConfig: RemoteConfigRepository,
    private val createGroup: GroupCreateUseCase,
    private val refreshEverythingUseCase: RefreshEverythingUseCase
) : CoreViewModel() {

    private val groupFlow = MutableStateFlow<Payload<UserGroup>>(Payload.Loading)
    private val _uiState = MutableStateFlow<Payload<CreateGroupState>>(Payload.Loading)
    val uiState: StateFlow<Payload<CreateGroupState>> = _uiState

    private val _eventChannel = Channel<CreateGroupScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val groupNameLengthLimit = 30

    init {
        coroutineScope.launch {
            val value = when {
                isGroupCreate() -> null
                else -> groupRepo.getGroupWithoutMembers(groupId!!)
                    .map { Payload.Success(it) }
                    .firstOrNull()
            } ?: Payload.Error(NullPointerException("No such group"))

            groupFlow.value = value
            _uiState.update {
                when {
                    isGroupCreate() -> {
                        val suggestions = listOf(
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.home),
                                name = MR.strings.group_create_suggestion_family.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.artMasks),
                                name = MR.strings.group_create_suggestion_friends.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.heart),
                                name = MR.strings.group_create_suggestion_couple.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.coffee),
                                name = MR.strings.group_create_suggestion_coffee.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.star),
                                name = MR.strings.group_create_suggestion_bestGroup.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.airplane),
                                name = MR.strings.group_create_suggestion_travel.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.schoolBooks),
                                name = MR.strings.group_create_suggestion_school.localizedString()
                            ),
                            NewGroupSuggestionChipData(
                                image = ImageModel.emoji(Emojis.suitecase),
                                name = MR.strings.group_create_suggestion_work.localizedString()
                            )
                        )
                        Payload.Success(CreateGroupState.empty(suggestions = suggestions))
                    }

                    else -> value.mapSuccess { group ->
                        CreateGroupState(
                            isNewGroup = false,
                            name = group.name,
                            image = group.image,
                            hasNameError = false,
                            isLoading = false,
                            alertDialog = it.data?.alertDialog,
                            suggestions = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: CreateGroupScreenUiEvent) {
        when (event) {
            CreateGroupScreenUiEvent.CreateGroup -> {
                tryCreateGroup()
            }

            is CreateGroupScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.PlanLimit, is MainAlertDialog.PlanLimitForMembers -> {
                        _uiState.update {
                            it.mapSuccess { it.copy(alertDialog = null) }
                        }
                    }

                    else -> {}
                }
            }

            is CreateGroupScreenUiEvent.OnGroupNameChange -> updateName(event.name)
            is CreateGroupScreenUiEvent.OnGroupSuggestionClicked -> {
                updateImage(event.suggestionChipData.image)
                updateName(event.suggestionChipData.name)
            }
        }
    }

    private fun tryCreateGroup() {

        val uiState = uiState.value
        if (uiState !is Payload.Success || uiState.data.isLoading) {
            return
        }

        val filteredName = uiState.data.name.trim().replace("\n", " ")

        if (filteredName.isBlank()) {
            _uiState.update {
                it.mapSuccess { it.copy(hasNameError = true) }
            }
            return
        }

        val group = groupFlow.value.data
        val image = uiState.data.image

        if (group != null) {
            val newData = when (group.data) {
                is CryptoResource.Ready, is CryptoResource.WrongKeyOrData, is CryptoResource.NotInitialized -> {
                    CryptoResource.Ready(
                        group.data.data?.copy(name = filteredName, image = image)
                            ?: UserGroupData(filteredName, image, UserGroup.groupPrivateGroupId)
                    )
                }

                else -> group.data
            }
            val newGroup = group.copy(data = newData)
            createGroup.updateGroup(newGroup)
                .map { group.id }
                .processGroupFlow()
        } else {
            coroutineScope.launch {
                if (checkGroupCreateLimits()) {
                    createGroup.createGroup(filteredName, image)
                        .processGroupFlow()
                }
            }
        }
    }

    private fun Flow<String>.processGroupFlow() {
        this
            .mapToPayload { it }
            .onEach { data ->
                when (data) {
                    is Payload.Error -> {
                        if (data.exception.asRemoteException()?.code == 402) {
                            if (checkGroupCreateLimits()) {
                                // if limits are still somehow ok, refresh all groups and show error
                                // this can happen in rare cases if request reaches the backed but response fails
                                // i.e. device gets disconnected in that exact moment
                                refreshEverythingUseCase.scheduleRefreshAllGroupsWithPlaces()
                                showSnackbar(data.exception.errorString())
                            }
                        } else {
                            showSnackbar(data.exception.errorString())
                        }
                    }

                    Payload.Loading -> {}
                    is Payload.Success -> {
                        _eventChannel.send(CreateGroupScreenEvent.GroupUpdated(data.data))
                    }
                }

                _uiState.update {
                    it.mapSuccess {
                        it.copy(
                            isLoading = data is Payload.Loading,
                            hasNameError = false
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun updateImage(image: ImageModel) {
        _uiState.update {
            it.mapSuccess { it.copy(image = image) }
        }
    }

    private fun updateName(name: String) {
        if (name.trim().length > groupNameLengthLimit)
            return
        _uiState.update {
            it.mapSuccess { it.copy(name = name, hasNameError = false) }
        }
    }

    fun isGroupCreate(): Boolean {
        return groupId.isNullOrBlank()
    }

    private suspend fun checkGroupCreateLimits(): Boolean {
        val limit = userRepo.userPlans.value.groupsLimit
        val groupCount =
            groupRepo.allGroupsState.value.data?.filter { it.group.isCurrentUserOwner }?.size ?: 0
        val maxGroupLimit =
            remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.groupsLimit

        if (maxGroupLimit != null && groupCount >= maxGroupLimit) {
            _uiState.update {
                it.mapSuccess {
                    it.copy(
                        alertDialog = MainAlertDialog.PlanLimit(
                            content = MR.strings.plan_limit_groups_alert_info.localizedString(),
                            limit = limit
                        )
                    )
                }
            }
            return false
        } else if (limit in 0..groupCount) {
            showSnackbar(MR.strings.dashboard_groups_create_plan_limit.localizedString())
            return false
        } else {
            return true
        }
    }
}