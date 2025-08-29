package app.paralino.core.presentation.login.password_confirmation

import app.paralino.core.MR
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.PasswordConfirmationUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.exception
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class PasswordConfirmationViewModel(
    private val passwordConfirmationUseCase: PasswordConfirmationUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val appAccountStateMonitor: AppAccountStateMonitor
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(PasswordConfirmationScreenState())
    val uiState: StateFlow<PasswordConfirmationScreenState> = _uiState

    private val _eventChannel = Channel<PasswordConfirmationScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private fun confirmPassword() {
        val currentState = _uiState.value
        if (currentState.actionLoading) return

        if (!validatePassword()) {
            return
        }

        passwordConfirmationUseCase.confirmPasswordAndRefreshKey(currentState.password)
            .mapToPayload { it }
            .onEach { result ->
                when (result) {
                    is Payload.Loading -> {
                        _uiState.update {
                            it.copy(confirmLoading = true)
                        }
                    }
                    is Payload.Success -> {
                        _uiState.update {
                            it.copy(confirmLoading = false)
                        }
                        // Trigger state check to update account state
                        appAccountStateMonitor.triggerStateCheck()
                    }
                    is Payload.Error -> {
                        _uiState.update {
                            it.copy(confirmLoading = false)
                        }
                        if (result.exception is DecryptionException) {
                            showSnackbar(MR.strings.recover_account_header_recover_possible.localizedString())
                        } else {
                            showSnackbar(result.exception.errorString())
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun logout() {
        val currentState = _uiState.value
        if (currentState.actionLoading) return

        flow {
            logoutUseCase.fullLogout(logoutLocallyOnError = false)
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { state ->
                state.exception?.let { showSnackbar(it.errorString()) }
                _uiState.update {
                    it.copy(logoutLoading = state is Payload.Loading)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun validatePassword(): Boolean {
        val currentState = _uiState.value
        val password = currentState.password
        val passwordValidation = FormValidation.TextNotEmpty.validate(password)

        _uiState.update {
            it.copy(hasPasswordError = !passwordValidation.result)
        }

        if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        }

        return passwordValidation.result
    }

    private fun onPasswordChange(password: String) {
        val currentState = _uiState.value
        if (currentState.actionLoading) return

        _uiState.update {
            it.copy(
                password = password,
                hasPasswordError = false
            )
        }
    }

    fun onEvent(event: PasswordConfirmationScreenUiEvent) {
        if (passwordConfirmationUseCase.isProcessingAny() || logoutUseCase.isProcessingAny()) {
            return
        }

        when (event) {
            is PasswordConfirmationScreenUiEvent.DialogAction -> {
                _uiState.update {
                    it.copy(alertDialog = null)
                }
                when (event.action.dialog) {
                    is MainAlertDialog.Logout -> {
                        if (event.action.isOk) {
                            logout()
                        }
                    }
                    else -> {}
                }
            }

            PasswordConfirmationScreenUiEvent.Logout -> {
                _uiState.update {
                    it.copy(alertDialog = MainAlertDialog.Logout)
                }
            }

            PasswordConfirmationScreenUiEvent.ConfirmPassword -> {
                confirmPassword()
            }

            is PasswordConfirmationScreenUiEvent.EditPassword -> {
                onPasswordChange(event.password)
            }
        }
    }
}
