package app.paralino.core.presentation.login

import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel constructor(
    private val loginUseCase: LoginUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    startScreenUiType: LoginScreenUiType,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow(LoginScreenState(uiType = startScreenUiType))
    val uiState: Flow<LoginScreenState> = _uiState

    private val _eventChannel = Channel<LoginScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> login()
            LoginScreenUiType.SIGNUP -> signUp()
            LoginScreenUiType.SIGNUP_GUEST -> signUpAsGuest()
            LoginScreenUiType.FORGOT_PASSWORD -> resetPassword()
        }
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
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(emailValidation))
            }
        } else if (!passwordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(passwordValidation))
            }
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
                _eventChannel.send(LoginScreenEvent.ValidationError(emailValidation))
            }
        } else if (!passwordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(passwordValidation))
            }
        } else if (!passwordConfirmValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(passwordConfirmValidation))
            }
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
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(emailValidation))
            }
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
            if(_uiState.value.uiType == LoginScreenUiType.FORGOT_PASSWORD) {
                _eventChannel.send(LoginScreenEvent.PasswordResetDone)
            } else {
                _eventChannel.send(LoginScreenEvent.UserReady)
            }
        }
    }

    private suspend fun processSignUpError(error: Throwable) {
        val uiType = _uiState.value.uiType
        val step = (error as? SignUpStepException)?.step

        val stepInfo = when (step) {
            SignUpStep.STARTING -> when (uiType) {
                LoginScreenUiType.LOGIN -> SignUpStepInfo.Error.LOGIN
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
            is UnauthorizedException -> SignUpErrorType.CREDENTIALS
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

        _eventChannel.send(
            LoginScreenEvent.LoginError(
                errorType
                    ?: SignUpErrorType.SOURCE(
                        (error as? SignUpStepException)?.cause ?: error
                    )
            )
        )

    }

    private fun resetValidationState() {
        _uiState.update {
            it.copy(hasEmailError = false, hasPasswordError = false, hasPasswordConfirmError = false)
        }
    }

    fun setScreenUiType(type: LoginScreenUiType) {
        if (_uiState.value.actionLoading)
            return

        if(type == _uiState.value.uiType)
            return

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
                it.copy(uiType = LoginScreenUiType.FORGOT_PASSWORD, password = "", passwordConfirm = "", stepInfo = null)
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
        _uiState.update { it.copy(passwordConfirm = password, hasPasswordConfirmError = false, stepInfo = null) }
    }

}