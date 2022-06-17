package com.zbapp.core.presentation.settings.passwordchange

import com.zbapp.core.domain.user.ChangePasswordUseCase
import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.common.FormValidation
import com.zbapp.core.utils.exceptions.InvalidRequestException
import com.zbapp.core.utils.exceptions.UnauthorizedException
import com.zbapp.core.utils.exceptions.asRemoteException
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val changePassword: ChangePasswordUseCase,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow(ChangePasswordScreenState())
    val uiState: Flow<ChangePasswordScreenState> = _uiState

    private val _eventChannel = Channel<ChangePasswordScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        val passwordOld = _uiState.value.oldPassword
        val passwordNew = _uiState.value.newPassword
        val passwordConfirm = _uiState.value.confirmPassword

        val oldPasswordValidation = FormValidation.Password.validate(passwordOld)
        val newPasswordValidation = FormValidation.Password.validate(passwordNew)
        val confirmPasswordValidation =
            FormValidation.PasswordConfirm.validate(passwordNew, passwordConfirm)

        _uiState.update {
            it.copy(
                hasOldPasswordError = !oldPasswordValidation.result,
                hasNewPasswordError = !newPasswordValidation.result,
                hasConfirmPasswordError = !confirmPasswordValidation.result
            )
        }

        val firstValidation =
            arrayOf(oldPasswordValidation, newPasswordValidation, confirmPasswordValidation)
                .firstOrNull { !it.result }

        if (firstValidation != null) {
            coroutineScope.launch {
                _eventChannel.send(ChangePasswordScreenEvent.ValidationError(firstValidation))
            }
        } else {
            changePassword.changePassword(oldPassword = passwordOld, newPassword = passwordNew)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }
                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null) }
                            if (result.exception.asRemoteException() is InvalidRequestException) {
                                _eventChannel.send(ChangePasswordScreenEvent.RemotePasswordError)
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                _eventChannel.send(ChangePasswordScreenEvent.CredentialsError)
                            } else {
                                _eventChannel.send(ChangePasswordScreenEvent.Error(result.exception))
                            }
                        }
                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = false) }
                            delay(400)
                            _eventChannel.send(ChangePasswordScreenEvent.PasswordChanged)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
    }


    fun onOldPasswordChange(password: String) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(oldPassword = password, hasOldPasswordError = false) }
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