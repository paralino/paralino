package app.paralino.core.presentation.settings.emailchange

import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeEmailUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class ChangeEmailViewModel(
    private val userRepository: UserRepository,
    private val changeEmail: ChangeEmailUseCase,
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow(ChangeEmailScreenState(userRepository.cachedUser?.email ?: ""))
    val uiState: Flow<ChangeEmailScreenState> = _uiState

    private val _eventChannel = Channel<ChangeEmailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun proceed() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.Password.validate(password)

        _uiState.update {
            it.copy(
                hasPasswordError = !passwordValidation.result,
                hasEmailError = !emailValidation.result
            )
        }

        if (!emailValidation.result) {
            showSnackbar(getFormValidationErrorText(validation = emailValidation.type))
        } else if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(validation = passwordValidation.type))
        } else {
            changeEmail.changeEmail(email = email, password = password)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }

                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null) }
                            if (result.exception.asRemoteException() is ConflictException) {
                                showSnackbar(MR.strings.form_invalid_email_conflict_description.localizedString())
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else {
                                showSnackbar(result.exception.errorString())
                            }
                        }

                        is Payload.Success -> {
                            _uiState.update { it.copy(actionLoading = false) }
                            delay(400)
                            _eventChannel.send(ChangeEmailScreenEvent.EmailChanged)
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

}