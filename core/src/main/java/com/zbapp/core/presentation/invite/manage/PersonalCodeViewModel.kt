package com.zbapp.core.presentation.invite.manage

import com.zbapp.core.models.Payload
import com.zbapp.core.models.mapSuccess
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.repo.personal_code.PersonalCodesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class PersonalCodeViewModel(
    private val repo: PersonalCodesRepository,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<PersonalCodeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PersonalCodeScreenState>> = _uiState

    private val _eventChannel = Channel<PersonalCodeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        repo.getMyPersonalCodes()
            .onEach { result ->
                _uiState.update {
                    result.mapSuccess {
                        PersonalCodeScreenState(
                            code = it.firstOrNull(),
                            createLoading = false,
                            actionLoading = false
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun createPersonalCode() {
        repo.createPersonalCode()
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