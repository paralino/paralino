package app.paralino.core.presentation.groups

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class GroupSettingsViewModel(
    val groupId: String,
    private val userRepo: UserRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val groupRemove: GroupRemoveUseCase,
) : CoreViewModel() {

    private val groupData = MutableStateFlow<UserGroup?>(null)
    private val _uiState =
        MutableStateFlow<Resource<GroupSettingsScreenState>>(Resource.NotInitialized)
    val uiState: Flow<Resource<GroupSettingsScreenState>> = _uiState

    private val _eventChannel = Channel<GroupSettingsScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            groupLocalDataSource.getGroupWithoutMembers(groupId),
            userRepo.getBlocklistLocally()
        ) { group, blocklist ->
            groupData.update { group }

            GroupSettingsScreenState(
                id = groupId,
                canEdit = group.isCurrentUserOwner,
                loadingAction = false,
                name = group.name,
                image = group.image,
                alertDialog = _uiState.value.data?.alertDialog
            )
        }
            .mapToPayload { it }
            .onEach { result ->
                when (result) {
                    is Payload.Success -> {
                        _uiState.update { Resource.Ready(result.data) }
                    }

                    is Payload.Error -> {
                        if (result.exception.asRemoteException() is NotFoundException) {
                            showSnackbar(MR.strings.group_load_error.localizedString())
                        } else {
                            showSnackbar(result.exception.errorString())
                        }
                    }

                    Payload.Loading -> {}
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: GroupSettingsUiEvent) {
        when (event) {
            GroupSettingsUiEvent.BlockGroup -> updateDialog(MainAlertDialog.BlockGroup)
            GroupSettingsUiEvent.DeleteGroup -> updateDialog(MainAlertDialog.DeleteGroup)
            GroupSettingsUiEvent.LeaveGroup -> updateDialog(MainAlertDialog.LeaveGroup)
            is GroupSettingsUiEvent.DialogAction -> {
                updateDialog(null)
                when (event.action.dialog) {
                    is MainAlertDialog.DeleteGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            leaveOrDeleteGroup()
                        }
                    }

                    is MainAlertDialog.LeaveGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            leaveOrDeleteGroup()
                        }
                    }

                    is MainAlertDialog.BlockGroup -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            blockGroup()
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun updateDialog(dialog: MainAlertDialog?) {
        _uiState.update { it.mapReady { it.copy(alertDialog = dialog) } }
    }

    private fun leaveOrDeleteGroup() {
        processGroupRemoval { data ->
            if (data.canEdit) {
                groupRemove.deleteGroup(groupId).mapToPayload { it }
            } else {
                groupRemove.leaveGroup(groupId).mapToPayload { it }
            }
        }
    }

    private fun blockGroup() {
        processGroupRemoval { groupRemove.blockGroup(groupId).mapToPayload { it } }
    }

    private fun processGroupRemoval(
        action: (GroupSettingsScreenState) -> Flow<Payload<Unit>>
    ) {
        val data = _uiState.value.data

        if (data == null) {
            showSnackbar(NotFoundException("Group not loaded").errorString())
            flowOf(Payload.Error<Unit>(NotFoundException("Group not loaded")))
        } else {
            action(data)
        }
            .onEach { result ->
                _uiState.update {
                    it.mapReady { it.copy(loadingAction = result is Payload.Loading) }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        showSnackbar(result.exception.errorString())
                    }

                    is Payload.Success -> {
                        _eventChannel.send(GroupSettingsScreenEvent.GroupDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }
}