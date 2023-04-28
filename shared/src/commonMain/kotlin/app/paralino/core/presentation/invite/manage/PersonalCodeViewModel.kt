package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class PersonalCodeViewModel(
    private val repo: PersonalCodesRepository,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PersonalCodeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PersonalCodeScreenState>> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        repo.getAndRefreshMyPersonalCodes()
            .onEach { result ->
                _uiState.update {

                    val codeData = result.firstOrNull()?.let {
                        PersonalCodeScreenCodeData(
                            id = it.id,
                            code = it.formattedCode ?: "---",
                            canDelete = true,
                            validityUTC = null
                        )
                    }

                    Payload.Success(
                        PersonalCodeScreenState(
                            code = codeData,
                            createLoading = false,
                            actionLoading = false
                        )
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    fun createPersonalCode() {
        flow { emit(repo.createAndSavePersonalCode()) }
            .mapToPayload { Unit }
            .onEach { result ->
                _uiState.update {
                    it.mapSuccess { it.copy(createLoading = result is Payload.Loading) }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                    }
                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeCreated)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun deletePersonalCode(codeId: String) {
        repo.revokePersonalCode(codeId)
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update { it.mapSuccess { it.copy(actionLoading = true) } }
                    }
                    is Payload.Error -> {
                        _eventChannel.send(PersonalCodeScreenEvent.Error(result.exception))
                        _uiState.update { it.mapSuccess { it.copy(actionLoading = false) } }
                    }
                    is Payload.Success -> {
                        _eventChannel.send(PersonalCodeScreenEvent.CodeDeleted)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

}