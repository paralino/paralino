package app.paralino.core.presentation.settings.convertaccount

import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class ConvertAccountViewModel(
    private val convertAccount: ConvertAccountUseCase,
): CoreViewModel() {

    private val _uiState = MutableStateFlow(ConvertAccountScreenState())
    val uiState: Flow<ConvertAccountScreenState> = _uiState

    private val _eventChannel = Channel<ConvertAccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun proceed() {
        if (_uiState.value.actionLoading == true) return

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
            showSnackbar(getFormValidationErrorText(validation = emailValidation.type))
        } else if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(validation = passwordValidation.type))
        } else if (!passwordConfirmValidation.result) {
            showSnackbar(getFormValidationErrorText(validation = passwordConfirmValidation.type))
        } else if (!convertAccount.isProcessingAny()){
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
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else if (result.exception.asRemoteException() is ConflictException) {
                                showSnackbar(MR.strings.form_invalid_email_conflict_description.localizedString())
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else if (result.exception is DecryptionException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else {
                                showSnackbar(result.exception.errorString())
                            }
                        }
                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = result.data != ChangeCredentialsStep.DONE) }
                            if (result.data == ChangeCredentialsStep.DONE) {
                                delay(400)
                                _eventChannel.send(ConvertAccountScreenEvent.AccountUpdated)
                            }
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