package com.zbapp.core.presentation.login.passwordreset

import com.zbapp.core.domain.user.ResetPasswordUseCase
import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.common.FormValidation
import com.zbapp.core.utils.exceptions.DecryptionException
import com.zbapp.core.utils.exceptions.InvalidRequestException
import com.zbapp.core.utils.exceptions.UnauthorizedException
import com.zbapp.core.utils.exceptions.asRemoteException
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ResetPasswordViewModel constructor(
    private val userId: String,
    private val secret: String,
    private val resetPassword: ResetPasswordUseCase,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow(ResetPasswordScreenState())
    val uiState: Flow<ResetPasswordScreenState> = _uiState

    private val _eventChannel = Channel<ResetPasswordScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        val passwordNew = _uiState.value.newPassword
        val passwordConfirm = _uiState.value.confirmPassword

        val newPasswordValidation = FormValidation.Password.validate(passwordNew)
        val confirmPasswordValidation =
            FormValidation.PasswordConfirm.validate(passwordNew, passwordConfirm)

        _uiState.update {
            it.copy(
                hasNewPasswordError = !newPasswordValidation.result,
                hasConfirmPasswordError = !confirmPasswordValidation.result
            )
        }

        val firstValidation =
            arrayOf(newPasswordValidation, confirmPasswordValidation)
                .firstOrNull { !it.result }

        if (firstValidation != null) {
            coroutineScope.launch {
                _eventChannel.send(ResetPasswordScreenEvent.ValidationError(firstValidation))
            }
        } else {
            resetPassword.resetPassword(userId = userId, secret = secret, password = passwordNew)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }
                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null) }
                            if (result.exception.asRemoteException() is InvalidRequestException) {
                                _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                            } else if (result.exception is DecryptionException) {
                                _eventChannel.send(ResetPasswordScreenEvent.CredentialsError)
                            } else {
                                _eventChannel.send(ResetPasswordScreenEvent.Error(result.exception))
                            }
                        }
                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = false) }
                            delay(400)
                            _eventChannel.send(ResetPasswordScreenEvent.PasswordChanged)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
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