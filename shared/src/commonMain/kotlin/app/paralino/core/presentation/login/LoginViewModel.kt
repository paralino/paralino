package app.paralino.core.presentation.login

import app.paralino.core.MR
import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class LoginViewModel constructor(
    private val loginUseCase: LoginUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val appConfigRepository: AppConfigRepository,
    private val startScreenUiType: LoginScreenUiType,
    private val config: AppConfig
) : CoreViewModel() {

    val initialUiType: LoginScreenUiType
        get() = if (appConfigRepository.configState.value?.isEmailEnabled == true) startScreenUiType else LoginScreenUiType.SIGNUP_GUEST

    private val _uiState: MutableStateFlow<LoginScreenState>
    val uiState: StateFlow<LoginScreenState>

    private val _eventChannel = Channel<LoginScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        _uiState = MutableStateFlow(
            LoginScreenState(
                canUseEmail = appConfigRepository.configState.value?.isEmailEnabled ?: false,
                uiType = initialUiType,
                alertDialog = if (config.hasGuestAccount()) {
                    MainAlertDialog.GuestAccountRecover
                } else {
                    null
                }
            )
        )
        uiState = _uiState

        appConfigRepository.getConfig()
            .onEach { config ->
                _uiState.update {
                    it.copy(
                        canUseEmail = config.isEmailEnabled,
                        uiType = if (!config.isEmailEnabled) LoginScreenUiType.SIGNUP_GUEST else it.uiType
                    )
                }
            }
            .launchIn(coroutineScope)
    }

    private fun proceed() {

        if (_uiState.value.uiType != LoginScreenUiType.SIGNUP_GUEST && !_uiState.value.canUseEmail) {
            _uiState.update { it.copy(uiType = LoginScreenUiType.SIGNUP_GUEST) }
            return
        }

        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> login()
            LoginScreenUiType.SIGNUP -> signUp()
            LoginScreenUiType.SIGNUP_GUEST -> signUpAsGuest()
            LoginScreenUiType.FORGOT_PASSWORD -> resetPassword()
        }
    }

    private fun recoverGuestAccount() {
        _uiState.update {
            it.copy(
                uiType = LoginScreenUiType.LOGIN,
                alertDialog = null,
                email = "***",
                password = "***"
            )
        }
        loginUseCase.login(email = config.guestEmail, password = config.guestPassword)
            .onEach { processStep(it) }
            .catch {

                processSignUpError(it, onRecoverGuest = true)

                val cause = it.cause
                if (cause is UnauthorizedException && cause.type == UserSessionMonitor.invalidCredentials) {
                    dismissGuestAccount()
                    _uiState.update {
                        it.copy(
                            uiType = LoginScreenUiType.SIGNUP,
                            email = "",
                            password = ""
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            uiType = LoginScreenUiType.SIGNUP_GUEST,
                            email = "",
                            password = "",
                            alertDialog = MainAlertDialog.GuestAccountRecover
                        )
                    }
                }

                delay(2000)
                _uiState.update { it.copy(stepInfo = null) }
            }
            .launchIn(coroutineScope)
    }

    private fun login() {
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
            showSnackbar(getFormValidationErrorText(emailValidation.type))
        } else if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        } else {
            loginUseCase.login(email = email, password = password)
                .onEach { processStep(it) }
                .catch { processSignUpError(it) }
                .launchIn(coroutineScope)
        }
    }

    private fun signUp() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        val passwordConfirm = _uiState.value.passwordConfirm

        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.Password.validate(password)
        val passwordConfirmValidation =
            FormValidation.PasswordConfirm.validate(password, passwordConfirm)

        _uiState.update {
            it.copy(
                hasPasswordError = !passwordValidation.result,
                hasPasswordConfirmError = !passwordConfirmValidation.result,
                hasEmailError = !emailValidation.result
            )
        }

        if (!emailValidation.result) {
            showSnackbar(getFormValidationErrorText(emailValidation.type))
        } else if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        } else if (!passwordConfirmValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordConfirmValidation.type))
        } else {
            createAccountUseCase.signUp(
                email = email,
                password = password
            )
                .onEach { processStep(it) }
                .catch { processSignUpError(it) }
                .launchIn(coroutineScope)
        }
    }

    private fun signUpAsGuest() {
        _uiState.update {
            it.copy(
                email = "",
                password = "",
                passwordConfirm = "",
                hasEmailError = false,
                hasPasswordError = false,
                hasPasswordConfirmError = false,
                stepInfo = null
            )
        }

        createAccountUseCase.signUpAsGuest()
            .onEach { processStep(it) }
            .catch { processSignUpError(it) }
            .launchIn(coroutineScope)
    }

    private fun resetPassword() {
        val email = _uiState.value.email
        val emailValidation = FormValidation.Email.validate(email)

        _uiState.update {
            it.copy(
                hasPasswordError = false,
                hasEmailError = !emailValidation.result
            )
        }

        if (!emailValidation.result) {
            showSnackbar(getFormValidationErrorText(emailValidation.type))
        } else {
            resetPasswordUseCase.initResetPassword(email)
                .onEach {
                    processStep(it)
                }
                .catch {
                    processSignUpError(it)
                }
                .launchIn(coroutineScope)
        }
    }

    private suspend fun processStep(step: SignUpStep) {
        _uiState.update {
            it.copy(
                actionLoading = step != SignUpStep.ALL_DONE,
                stepInfo = when (step) {
                    SignUpStep.STARTING -> when (it.uiType) {
                        LoginScreenUiType.LOGIN -> SignUpStepInfo.Info.START_LOGIN
                        LoginScreenUiType.SIGNUP, LoginScreenUiType.SIGNUP_GUEST -> SignUpStepInfo.Info.START_SIGNUP
                        LoginScreenUiType.FORGOT_PASSWORD -> SignUpStepInfo.Info.START_PASSWORD_RESET
                    }

                    SignUpStep.GENERATING_KEYS -> SignUpStepInfo.Info.CRYPTO_SETUP
                    SignUpStep.INIT_USER_DATA -> SignUpStepInfo.Info.USER_SETUP
                    SignUpStep.SAVE_USER -> SignUpStepInfo.Info.USER_SETUP
                    SignUpStep.ALL_DONE -> SignUpStepInfo.Info.DONE
                }
            )
        }
        if (step == SignUpStep.ALL_DONE) {
            if (_uiState.value.uiType == LoginScreenUiType.FORGOT_PASSWORD) {
                _eventChannel.send(LoginScreenEvent.PasswordResetDone)
            } else {
                _eventChannel.send(LoginScreenEvent.UserReady)
            }
        }
    }

    private suspend fun processSignUpError(error: Throwable, onRecoverGuest: Boolean = false) {
        val uiType = _uiState.value.uiType
        val step = (error as? SignUpStepException)?.step

        val stepInfo = when (step) {
            SignUpStep.STARTING -> when (uiType) {
                LoginScreenUiType.LOGIN -> {
                    if (onRecoverGuest) SignUpStepInfo.Error.RECOVER_GUEST
                    else SignUpStepInfo.Error.LOGIN
                }

                LoginScreenUiType.SIGNUP, LoginScreenUiType.SIGNUP_GUEST -> SignUpStepInfo.Error.SIGN_UP
                LoginScreenUiType.FORGOT_PASSWORD -> SignUpStepInfo.Error.PASSWORD_RESET
            }

            SignUpStep.GENERATING_KEYS -> SignUpStepInfo.Error.CRYPTO
            SignUpStep.INIT_USER_DATA -> SignUpStepInfo.Error.USER_DATA
            SignUpStep.SAVE_USER -> SignUpStepInfo.Error.USER_DATA
            SignUpStep.ALL_DONE -> null
            else -> null
        }

        _uiState.update { it.copy(stepInfo = stepInfo, actionLoading = false) }

        // extra error handling
        val errorType = when (error.cause) {
            is ConflictException -> SignUpErrorType.ACCOUNT_CONFLICT
            is UnauthorizedException -> {
                if (onRecoverGuest) SignUpErrorType.CREDENTIALS_GUEST
                else SignUpErrorType.CREDENTIALS
            }

            is CryptoException -> {
                when (step) {
                    SignUpStep.GENERATING_KEYS,
                    SignUpStep.INIT_USER_DATA,
                    -> {
                        when (uiType) {
                            LoginScreenUiType.LOGIN -> SignUpErrorType.LOGIN_CRYPTO
                            LoginScreenUiType.SIGNUP -> SignUpErrorType.SIGNUP_CRYPTO
                            else -> null
                        }
                    }

                    else -> null
                }
            }

            else -> {
                when (uiType) {
                    LoginScreenUiType.SIGNUP -> {
                        when (step) {
                            SignUpStep.SAVE_USER, SignUpStep.INIT_USER_DATA -> {
                                SignUpErrorType.SIGNUP_DATA
                            }

                            else -> null
                        }
                    }

                    else -> null
                }
            }
        }

        showSnackbar(
            getLoginErrorText(
                errorType ?: SignUpErrorType.SOURCE(
                    error = (error as? SignUpStepException)?.cause ?: error
                )
            )
        )
    }

    private fun resetValidationState() {
        _uiState.update {
            it.copy(
                hasEmailError = false,
                hasPasswordError = false,
                hasPasswordConfirmError = false
            )
        }
    }

    fun setScreenUiType(type: LoginScreenUiType) {
        if (_uiState.value.actionLoading)
            return

        if (type == _uiState.value.uiType)
            return

        if (type != LoginScreenUiType.SIGNUP_GUEST && !_uiState.value.canUseEmail) {
            if (!config.hasGuestAccount()) {
                _uiState.update {
                    it.copy(alertDialog = MainAlertDialog.ComingSoon)
                }
            }
            return
        }

        resetValidationState()

        _uiState.update {
            it.copy(uiType = type, stepInfo = null)
        }
    }

    fun toggleLoginSignup() {
        if (_uiState.value.actionLoading)
            return

        resetValidationState()
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.SIGNUP, stepInfo = null)
            }

            LoginScreenUiType.SIGNUP, LoginScreenUiType.SIGNUP_GUEST -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.LOGIN, stepInfo = null)
            }

            LoginScreenUiType.FORGOT_PASSWORD -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.LOGIN, stepInfo = null)
            }
        }
    }

    fun toggleForgotPassword() {
        if (_uiState.value.actionLoading)
            return

        resetValidationState()
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> _uiState.update {
                it.copy(
                    uiType = LoginScreenUiType.FORGOT_PASSWORD,
                    password = "",
                    passwordConfirm = "",
                    stepInfo = null
                )
            }

            LoginScreenUiType.FORGOT_PASSWORD -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.LOGIN, stepInfo = null)
            }

            else -> {
                /* do nothing */
            }
        }
    }

    fun onEmailChange(email: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(email = email, hasEmailError = false, stepInfo = null) }
    }

    fun onPasswordChange(password: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(password = password, hasPasswordError = false, stepInfo = null) }
    }

    fun onPasswordConfirmChange(password: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update {
            it.copy(
                passwordConfirm = password,
                hasPasswordConfirmError = false,
                stepInfo = null
            )
        }
    }

    private fun dismissGuestAccount() {
        config.guestEmail = ""
        config.guestPassword = ""
        _uiState.update { it.copy(alertDialog = null) }
    }

    fun onEvent(event: LoginScreenUiEvent) {
        when (event) {
            LoginScreenUiEvent.LoginOrSignup -> {
                if (_uiState.value.uiType == LoginScreenUiType.SIGNUP_GUEST) {
                    _uiState.update { it.copy(alertDialog = MainAlertDialog.GuestAccountCreating) }
                } else {
                    proceed()
                }
            }

            is LoginScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.ComingSoon -> {
                        hideDialog()
                    }
                    is MainAlertDialog.GuestAccountCreating -> {
                        hideDialog()
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            proceed()
                        }
                    }

                    is MainAlertDialog.GuestAccountRecover -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            recoverGuestAccount()
                        } else if (event.action.action == MainAlertDialog.Buttons.buttonCancel) {
                            _uiState.update { it.copy(alertDialog = MainAlertDialog.GuestAccountDismissConfirmation) }
                        }
                    }

                    is MainAlertDialog.GuestAccountDismissConfirmation -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            dismissGuestAccount()
                        } else if (event.action.action == MainAlertDialog.Buttons.buttonCancel) {
                            _uiState.update {
                                it.copy(
                                    alertDialog = if (config.hasGuestAccount()) {
                                        MainAlertDialog.GuestAccountRecover
                                    } else {
                                        null
                                    }
                                )
                            }
                        }
                    }
                     else -> {}
                }
            }
        }
    }

    private fun hideDialog() {
        if (!config.hasGuestAccount()) {
            _uiState.update { it.copy(alertDialog = null) }
        }
    }

    private fun getLoginErrorText(error: SignUpErrorType): String {
        return when (error) {
            SignUpErrorType.ACCOUNT_CONFLICT -> MR.strings.form_invalid_email_conflict_description.localizedString()
            SignUpErrorType.CREDENTIALS -> MR.strings.form_invalid_credentials_description.localizedString()
            SignUpErrorType.CREDENTIALS_GUEST -> MR.strings.login_step_error_recoverGuest.localizedString()
            SignUpErrorType.LOGIN_CRYPTO -> MR.strings.login_step_error_encryption_logIn.localizedString()
            SignUpErrorType.SIGNUP_CRYPTO -> MR.strings.login_step_error_encryption_signUp.localizedString()
            SignUpErrorType.SIGNUP_DATA -> MR.strings.login_step_error_user_setup_signUp.localizedString()
            is SignUpErrorType.SOURCE -> error.error.errorString()
        }
    }
}