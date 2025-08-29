package app.paralino.core.presentation.settings.passwordchange

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.domain.user.ChangeCredentialsStep
import app.paralino.core.domain.user.ChangeCredentialsUseCase
import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.RemoteConnectionException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

class ChangePasswordViewModel(
    private val changePassword: ChangeCredentialsUseCase,
    private val valueFormatter: IValueFormatter,
    private val userRepo: UserRepository,
    private val scopeProvider: ICoroutineScopeProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        ChangePasswordScreenState(
            lastPasswordUpdateTime = getLastPasswordUpdateDate()
        )
    )
    val uiState: StateFlow<ChangePasswordScreenState> = _uiState
    val canNavigateBack: Flow<Boolean> = _uiState.map {
        it.actionLoading != true
    }

    private val _eventChannel = Channel<ChangePasswordScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                withTimeout(5.seconds) {
                    userRepo.refreshUserIfAvailable()
                }
                _uiState.update {
                    it.copy(lastPasswordUpdateTime = getLastPasswordUpdateDate())
                }
            } catch (e: Exception) {
                showSnackbar(
                    RemoteConnectionException(
                        "Connection error",
                        code = null
                    ).errorString()
                )
            }
        }
    }

    fun proceed() {
        if (_uiState.value.actionLoading == true || changePassword.isProcessingAny()) return
        coroutineScope.launch {
            val passwordOld = _uiState.value.oldPassword
            val passwordNew = _uiState.value.newPassword
            val passwordConfirm = _uiState.value.confirmPassword

            val oldPasswordValidation = FormValidation.TextNotEmpty.validate(passwordOld)
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
            } else {
                changePasswordAction(
                    passwordOld = passwordOld,
                    passwordNew = passwordNew,
                    keepSessions = _uiState.value.keepDevicesLoggedIn
                )
            }
        }
    }

    private fun changePasswordAction(
        passwordOld: String,
        passwordNew: String,
        keepSessions: Boolean
    ){

        if (changePassword.isProcessingAny() || uiState.value.actionLoading == true) {
            return
        }

        changePassword.changePassword(
            oldPassword = passwordOld,
            newPassword = passwordNew,
            keepSessions = keepSessions
        )
            .mapToPayload { it }
            .onEach { result ->
                when (result) {
                    Payload.Loading -> {
                        _uiState.update { it.copy(actionLoading = true) }
                    }

                    is Payload.Error -> {
                        _uiState.update {
                            it.copy(
                                actionLoading = null,
                                credentialsStep = null
                            )
                        }
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
            .launchIn(scopeProvider.userSessionIOScope)
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

    fun onDeviceSessionToggleChange(enabled: Boolean) {
        if (_uiState.value.actionLoading == true) return
        _uiState.update { it.copy(keepDevicesLoggedIn = enabled) }
    }

    private fun getLastPasswordUpdateDate(): AnnotatedString? {
        return userRepo.cachedUser?.passwordUpdate?.let {
            valueFormatter.formatSimpleDateTime(
                timeUTC = it.toLocalDateTime(TimeZone.UTC),
                optionalYear = false,
                showTodayInfo = true
            )?.let { time ->
                MR.strings.group_members_last_status_update.localizedString() + " $time"
            }?.annotatedBold()
        }
    }
}