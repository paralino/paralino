package app.paralino.core.presentation.settings.passwordchange

import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.domain.user.ChangeCredentialsUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ChangePasswordViewModel(
    private val changePassword: ChangeCredentialsUseCase,
    private val valueFormatter: IValueFormatter,
    private val userRepo: UserRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        ChangePasswordScreenState(
            lastPasswordUpdateTime = userRepo.cachedUser?.passwordUpdate?.let {
                valueFormatter.formatSimpleDateTime(
                    timeUTC = it.toLocalDateTime(TimeZone.currentSystemDefault()),
                    optionalYear = false,
                    showTodayInfo = true
                )?.let { time ->
                    MR.strings.group_members_last_status_update.localizedString() + " $time"
                }?.annotatedBold()
            }
        )
    )
    val uiState: StateFlow<ChangePasswordScreenState> = _uiState

    private val _eventChannel = Channel<ChangePasswordScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun proceed() {
        if (_uiState.value.actionLoading == true || changePassword.isProcessingAny()) return

        val passwordOld = _uiState.value.oldPassword
        val passwordNew = _uiState.value.newPassword
        val passwordConfirm = _uiState.value.confirmPassword

        val oldPasswordValidation = FormValidation.TextNotEmpty.validate(passwordOld)
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

        val firstValidation = arrayOf(
            oldPasswordValidation,
            newPasswordValidation,
            confirmPasswordValidation
        )
            .firstOrNull { !it.result }

        if (firstValidation != null) {
            showSnackbar(getFormValidationErrorText(validation = firstValidation.type))
        } else if (!changePassword.isProcessingAny()){
            changePassword.changePassword(oldPassword = passwordOld, newPassword = passwordNew)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {
                            _uiState.update { it.copy(actionLoading = true) }
                        }
                        is Payload.Error -> {
                            _uiState.update { it.copy(actionLoading = null, credentialsStep = null) }
                            if (result.exception.asRemoteException() is InvalidRequestException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else if (result.exception.asRemoteException() is UnauthorizedException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else if (result.exception is DecryptionException) {
                                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
                            } else {
                                showSnackbar(result.exception.errorString())
                            }
                        }
                        is Payload.Success -> {
                            _uiState.update {
                                it.copy(
                                    actionLoading = result.data != ChangeCredentialsStep.DONE,
                                    credentialsStep = result.data
                                )
                            }
                            if (result.data == ChangeCredentialsStep.DONE) {
                                delay(400)
                                _eventChannel.send(ChangePasswordScreenEvent.PasswordChanged)
                            }
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