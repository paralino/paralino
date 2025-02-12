package app.paralino.core.presentation.login.passwordreset

import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel constructor(
    private val userId: String,
    private val secret: String,
    private val resetPassword: ResetPasswordUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordScreenState())
    val uiState: StateFlow<ResetPasswordScreenState> = _uiState

    private val _eventChannel = Channel<ResetPasswordScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        coroutineScope.launch {
            val email = _uiState.value.email

            val passwordNew = _uiState.value.newPassword
            val passwordConfirm = _uiState.value.confirmPassword

            val emailValidation = FormValidation.Email.validate(email)
            val newPasswordValidation = FormValidation.Password.validateWithDictionary(
                password = passwordNew,
                dictionary = FormValidation.Password.defaultPasswordDictionary()
            )
            val confirmPasswordValidation = FormValidation.PasswordConfirm.validate(
                password = passwordNew,
                confirmPassword = passwordConfirm
            )

            _uiState.update {
                it.copy(
                    hasEmailError = !emailValidation.result,
                    hasNewPasswordError = !newPasswordValidation.result,
                    hasConfirmPasswordError = !confirmPasswordValidation.result
                )
            }

            val firstValidation =
                arrayOf(emailValidation, newPasswordValidation, confirmPasswordValidation)
                    .firstOrNull { !it.result }

            if (firstValidation != null) {
                coroutineScope.launch {
                    _eventChannel.send(ResetPasswordScreenEvent.ValidationError(firstValidation))
                }
            } else {
                resetPasswordAction(email = email, password = passwordNew)
            }
        }
    }

    private fun resetPasswordAction(
        email: String,
        password: String
    ) {
        if (resetPassword.isProcessingAny() || uiState.value.actionLoading == true) {
            return
        }

        resetPassword.resetPassword(
            email = email,
            userId = userId,
            secret = secret,
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
                        val remoteError = result.exception.asRemoteException()
                        if (remoteError is InvalidRequestException) {
                            _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                        } else if (remoteError is UnauthorizedException) {
                            if (remoteError.type == BaseRemoteException.typeUserInvalidToken) {
                                _eventChannel.send(ResetPasswordScreenEvent.InvalidToken)
                            } else {
                                _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                            }
                        } else if (result.exception is DecryptionException) {
                            _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                        } else {
                            _eventChannel.send(ResetPasswordScreenEvent.Error(result.exception))
                        }
                    }

                    is Payload.Success -> {
                        if (result.data == SignUpStep.ALL_DONE) {
                            _uiState.update { it.copy(actionLoading = false) }
                            _eventChannel.send(ResetPasswordScreenEvent.PasswordChanged)
                        }
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEmailChange(email: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(email = email, hasEmailError = false) }
    }

    fun onNewPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(newPassword = password, hasNewPasswordError = false) }
    }

    fun onConfirmPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(confirmPassword = password, hasConfirmPasswordError = false) }
    }
}