package app.paralino.core.presentation.settings.blocklist

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BlocklistVideModel(
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
            .catch { _eventChannel.send(BlockListScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

    fun removeBlock(id: String) {
        val element = _uiState.value.items.firstOrNull { it.id == id } ?: return
        if (element.actionLoading == true) return

        coroutineScope.launch {
            updateLoading(id, loading = true)
            try {
                userRepository.removeFromBlocklist(id)
                updateLoading(id, loading = false)
            } catch (e: Exception) {
                updateLoading(id, loading = null)
                _eventChannel.send(BlockListScreenEvent.Error(e))
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
}