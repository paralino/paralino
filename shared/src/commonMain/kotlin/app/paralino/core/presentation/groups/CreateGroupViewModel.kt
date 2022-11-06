package app.paralino.core.presentation.groups

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val groupId: String?,
    private val groupRepo: GroupRepository,
    scope: CoroutineScope
) : CoreViewModel(scope) {


    private val groupFlow = MutableStateFlow<Payload<UserGroup>>(Payload.Loading)
    private val _uiState = MutableStateFlow<Payload<CreateGroupState>>(Payload.Loading)
    val uiState: Flow<Payload<CreateGroupState>> = _uiState

    private val _eventChannel = Channel<CreateGroupScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val groupNameLengthLimit = 30

    init {
        coroutineScope.launch {
            val value = when {
                groupId.isNullOrBlank() -> null
                else -> groupRepo.getGroupWithoutMembersLocally(groupId)
                    .map { Payload.Success(it) }
                    .firstOrNull()
            } ?: Payload.Error(NullPointerException("No such group"))

            groupFlow.value = value
            _uiState.update {
                when {
                    groupId.isNullOrBlank() -> Payload.Success(CreateGroupState.empty())
                    else -> value.mapSuccess { group ->
                        CreateGroupState(
                            isNewGroup = false,
                            name = group.name,
                            image = group.image,
                            hasError = false,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun createGroup(image: ImageModel, name: String) {

        val filteredName = name.trim().replace("\n", " ")

        if (filteredName.isBlank()) {
            _uiState.update {
                it.mapSuccess { it.copy(hasError = true) }
            }
            return
        }

        val group = groupFlow.value.data
        val groupUpdateFlow = if (group != null) {
            val newData = when(group.data) {
                is CryptoResource.Ready -> {
                    CryptoResource.Ready(group.data.data.copy(name = filteredName, image = image))
                }
                else -> group.data
            }
            val newGroup = group.copy(data = newData)
            groupRepo.updateGroup(newGroup)
        } else {
            groupRepo.createGroup(filteredName, image)
        }

        groupUpdateFlow
            .mapToPayload<Unit, Unit>()
            .onEach { data ->
                _uiState.update {
                    it.mapSuccess { it.copy(isLoading = data is Payload.Loading, hasError = false) }
                }
                when (data) {
                    is Payload.Error -> {
                        _eventChannel.send(CreateGroupScreenEvent.Error(data.exception))
                    }
                    Payload.Loading -> {}
                    is Payload.Success -> {
                        _eventChannel.send(CreateGroupScreenEvent.GroupUpdated)
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

}