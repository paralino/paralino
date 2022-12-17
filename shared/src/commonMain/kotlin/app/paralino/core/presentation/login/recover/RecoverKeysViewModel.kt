package app.paralino.core.presentation.login.recover

import app.paralino.core.config
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.models.*
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecoverKeysViewModel(
    val logoutUseCase: LogoutUseCase,
    val recoverAccountUseCase: RecoverAccountUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<RecoverKeysScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<RecoverKeysScreenState>> = _uiState

    private val _eventChannel = Channel<RecoverKeysScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private var keys = listOf<UserCryptoKeyRaw>()

    init {
        flow {
            emit(recoverAccountUseCase.getUserKeys())
        }
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { uiState ->
                    val backupPassword = config.temporaryBackupPassword
                    if (backupPassword.isNotBlank()) {
                        result.mapSuccess {
                            keys = it
                            RecoverKeysScreenState(
                                canRecoverOldKey = it.isNotEmpty(),
                                backupPassword = backupPassword
                            )
                        }
                    } else {
                        Payload.Error(MissingCryptoKeyException("Missing backup password"))
                    }
                }
            }
            .onEach {
                if(it is Payload.Error) {
                    _eventChannel.send(RecoverKeysScreenEvent.Error(it.exception))
                }
            }
            .launchIn(coroutineScope)
    }


    fun recoverAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        val key = validState.recoverState.data
        if (validatePassword()) {
            if (validState.canRecoverOldKey && key != null) {
                recoverAccountUseCase.recoverUserAccount(
                    backupPassword = validState.backupPassword,
                    key = key
                )
                    .mapToPayload { it }
                    .onEach { result ->

                        if (result is Payload.Error) {
                            _eventChannel.send(RecoverKeysScreenEvent.Error(result.exception))
                        }

                        _uiState.update {
                            it.mapSuccess {
                                it.copy(checkLoading = result is Payload.Loading)
                            }
                        }
                    }
                    .launchIn(coroutineScope)
            } else {
                coroutineScope.launch {
                    _eventChannel.send(RecoverKeysScreenEvent.CredentialsError)
                }
            }
        }
    }

    fun resetAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (!validState.canRecoverOldKey || validState.recoverState.data == null) {
            recoverAccountUseCase.resetUserAccount(
                backupPassword = validState.backupPassword
            )
                .mapToPayload { it }
                .onEach { result ->

                    if (result is Payload.Error) {
                        _eventChannel.send(RecoverKeysScreenEvent.Error(result.exception))
                    }

                    _uiState.update {
                        it.mapSuccess {
                            it.copy(resetLoading = result is Payload.Loading)
                        }
                    }
                }
                .launchIn(coroutineScope)
        } else {
            coroutineScope.launch {
                _eventChannel.send(RecoverKeysScreenEvent.CredentialsError)
            }
        }
    }

    fun checkForKey() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validatePassword()) {
            flow {
                val key =
                    recoverAccountUseCase.getCorrectUserKeyForPassword(keys, validState.oldPassword)
                emit(key)
            }
                .mapToPayload { it }
                .onEach { result ->

                    val state = when (result) {
                        Payload.Loading -> Resource.NotInitialized
                        is Payload.Success -> { Resource.Ready(result.data) }
                        is Payload.Error -> Resource.Ready(null)
                    }

                    _uiState.update {
                        it.mapSuccess {
                            it.copy(recoverState = state, checkLoading = result is Payload.Loading)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
    }

    private fun validatePassword(): Boolean {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return false
        val passwordOld = validState.oldPassword
        val oldPasswordValidation = FormValidation.Password.validate(passwordOld)

        _uiState.update {
            it.mapSuccess {
                it.copy(hasOldPasswordError = !oldPasswordValidation.result)
            }
        }

        if (!oldPasswordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(RecoverKeysScreenEvent.ValidationError(oldPasswordValidation))
            }
        }

        return oldPasswordValidation.result
    }

    fun logout() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validState.actionLoading) return
        logoutUseCase.logout()
            .mapToPayload { Unit }
            .onEach { state ->
                _eventChannel.send(state.exception?.let { RecoverKeysScreenEvent.Error(it) })
                _uiState.update {
                    it.mapSuccess {
                        it.copy(logoutLoading = state is Payload.Loading)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun onOldPasswordChange(password: String) {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validState.actionLoading) return
        _uiState.update {
            it.mapSuccess {
                it.copy(
                    oldPassword = password,
                    hasOldPasswordError = false,
                    recoverState = Resource.NotInitialized
                )
            }
        }
    }

}