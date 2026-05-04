package app.paralino.core.presentation.settings.blocklist

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BlocklistViewModel(
    private val userRepository: UserRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(BlocklistScreenState())
    val uiState: Flow<BlocklistScreenState> = _uiState

    private val _eventChannel = Channel<BlockListScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepository.getBlocklist()
            .onEach { blocklist ->
                _uiState.update {
                    it.copy(
                        groups = blocklist
                            .filter { it.resourceType == BlocklistType.GROUP }
                            .map {
                                BlocklistElementState(
                                    id = it.id,
                                    type = it.resourceType,
                                    name = it.data?.data?.name ?: "---",
                                    image = it.data?.data?.image ?: ImageModel.emoji(Emojis.robot)
                                )
                            },
                        users = blocklist
                            .filter { it.resourceType == BlocklistType.USER }
                            .map {
                                BlocklistElementState(
                                    id = it.id,
                                    type = it.resourceType,
                                    name = it.data?.data?.name ?: "---",
                                    image = it.data?.data?.image ?: ImageModel.emoji(Emojis.robot)
                                )
                            },
                        loading = false
                    )
                }
            }
            .catch { messageDispatcher.showSnackbar(it.errorString()) }
            .launchIn(coroutineScope)
    }

    private fun removeBlock(id: String) {
        val element = _uiState.value.items.firstOrNull { it.id == id } ?: return
        if (element.actionLoading == true) return

        coroutineScope.launch {
            updateLoading(id, loading = true)
            try {
                userRepository.removeFromBlocklist(id)
                updateLoading(id, loading = false)
            } catch (e: Exception) {
                updateLoading(id, loading = null)
                messageDispatcher.showSnackbar(e.errorString())
            }
        }
    }

    private fun updateLoading(elementId: String, loading: Boolean?) {
        _uiState.update {
            it.copy(
                groups = it.groups.map {
                    if (it.id == elementId)
                        it.copy(actionLoading = loading)
                    else it
                },
                users = it.users.map {
                    if (it.id == elementId)
                        it.copy(actionLoading = loading)
                    else it
                }
            )
        }
    }

    fun onEvent(event: BlockListScreenUiEvent) {
        when (event) {
            is BlockListScreenUiEvent.UnblockUserOrGroup -> {
                _uiState.update {
                    it.copy(
                        confirmationDialog = MainAlertDialog.UnblockGroupOrUser(
                            id = event.id,
                            type = event.type
                        )
                    )
                }
            }

            is BlockListScreenUiEvent.UnblockUserOrGroupConfirm -> {
                removeBlock(event.id)
            }

            BlockListScreenUiEvent.HideDialog -> {
                _uiState.update {
                    it.copy(confirmationDialog = null)
                }
            }
        }
    }
}