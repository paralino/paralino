package app.paralino.core.presentation.settings.convertaccount

import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.utils.exceptions.*
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConvertAccountViewModel(
    private val convertAccount: ConvertAccountUseCase,
): CoreViewModel() {

    private val _uiState = MutableStateFlow(ConvertAccountScreenState())
    val uiState: Flow<ConvertAccountScreenState> = _uiState

    private val _eventChannel = Channel<ConvertAccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun proceed() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        val passwordConfirm = _uiState.value.passwordConfirm

        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.Password.validate(password)
        val passwordConfirmValidation = FormValidation.PasswordConfirm.validate(password, passwordConfirm)

        _uiState.update {
            it.copy(
                hasPasswordError = !passwordValidation.result,
                hasPasswordConfirmError = !passwordConfirmValidation.result,
                hasEmailError = !emailValidation.result
            )
        }

        if (!emailValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(ConvertAccountScreenEvent.ValidationError(emailValidation))
            }
        } else if (!passwordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(ConvertAccountScreenEvent.ValidationError(passwordValidation))
            }
        } else if (!passwordConfirmValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(ConvertAccountScreenEvent.ValidationError(passwordConfirmValidation))
            }
        } else {
            convertAccount.convertAccount(
                email = email,
                password = password
            )
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }
                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null) }
                            if (result.exception.asRemoteException() is InvalidRequestException) {
                                _eventChannel.send(ConvertAccountScreenEvent.CredentialsError)
                            } else if (result.exception.asRemoteException() is ConflictException) {
                                _eventChannel.send(ConvertAccountScreenEvent.EmailError)
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                _eventChannel.send(ConvertAccountScreenEvent.CredentialsError)
                            } else if (result.exception is DecryptionException) {
                                _eventChannel.send(ConvertAccountScreenEvent.CredentialsError)
                            } else {
                                _eventChannel.send(ConvertAccountScreenEvent.Error(result.exception))
                            }
                        }
                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = false) }
                            delay(400)
                            _eventChannel.send(ConvertAccountScreenEvent.AccountUpdated)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
    }

    fun onEmailChange(email: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(email = email, hasEmailError = false) }
    }

    fun onPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(password = password, hasPasswordError = false) }
    }

    fun onConfirmPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(passwordConfirm = password, hasPasswordConfirmError = false) }
    }

}