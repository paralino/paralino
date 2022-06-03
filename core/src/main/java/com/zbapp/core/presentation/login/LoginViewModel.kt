package com.zbapp.core.presentation.login

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.common.FormValidation
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.SignUpStep
import com.zbapp.core.repo.user.SignUpStepException
import com.zbapp.core.utils.exceptions.ConflictException
import com.zbapp.core.utils.exceptions.CryptoException
import com.zbapp.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginManager: LoginManager,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow(LoginScreenState())
    val uiState: Flow<LoginScreenState> = _uiState

    private val _eventChannel = Channel<LoginScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    fun proceed() {
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> login()
            LoginScreenUiType.SIGNUP -> signUp()
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
            loginManager.login(email = email, password = password)
                .onEach { processStep(it) }
                .catch { processSignUpError(it) }
                .launchIn(coroutineScope)
        }
    }

    private fun signUp() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        val name = _uiState.value.userName

        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.Password.validate(password)
        val userNameValidation = FormValidation.TextNotEmpty.validate(name)

        _uiState.update {
            it.copy(
                hasPasswordError = !passwordValidation.result,
                hasEmailError = !emailValidation.result,
                hasNameError = !userNameValidation.result
            )
        }

        if (!userNameValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(userNameValidation))
            }
        } else if (!emailValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(emailValidation))
            }
        } else if (!passwordValidation.result) {
            coroutineScope.launch {
                _eventChannel.send(LoginScreenEvent.ValidationError(passwordValidation))
            }
        } else {
            loginManager.signUp(
                userName = name,
                userImage = _uiState.value.userImage,
                email = email,
                password = password
            )
                .onEach { processStep(it) }
                .catch { processSignUpError(it) }
                .launchIn(coroutineScope)
        }
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
            coroutineScope.launch {
                processStep(SignUpStep.STARTING)
                delay(2000)
                processStep(SignUpStep.ALL_DONE)
            }
        }
    }

    private suspend fun processStep(step: SignUpStep) {
        _uiState.update {
            it.copy(
                actionLoading = step != SignUpStep.ALL_DONE,
                stepInfo = when (step) {
                    SignUpStep.STARTING -> when (it.uiType) {
                        LoginScreenUiType.LOGIN -> SignUpStepInfo.Info.START_LOGIN
                        LoginScreenUiType.SIGNUP -> SignUpStepInfo.Info.START_SIGNUP
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
            _eventChannel.send(LoginScreenEvent.UserReady)
        }
    }

    private suspend fun processSignUpError(error: Throwable) {
        val uiType = _uiState.value.uiType
        val step = (error as? SignUpStepException)?.step

        val stepInfo = when (step) {
            SignUpStep.STARTING -> when (uiType) {
                LoginScreenUiType.LOGIN -> SignUpStepInfo.Error.LOGIN
                LoginScreenUiType.SIGNUP -> SignUpStepInfo.Error.SIGN_UP
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
                                // TODO maybe handle better
                                // account is created, but user data is not properly initialized
                                // switch to login and/or (offer) to recreate keys
                                toggleLoginSignup()
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
            it.copy(hasEmailError = false, hasPasswordError = false, hasNameError = false)
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
            LoginScreenUiType.SIGNUP -> _uiState.update {
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
                it.copy(uiType = LoginScreenUiType.FORGOT_PASSWORD, password = "", stepInfo = null)
            }
            LoginScreenUiType.FORGOT_PASSWORD -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.LOGIN, stepInfo = null)
            }
            else -> {
                /* do nothing */
            }
        }
    }

    fun onUserNameChange(name: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(userName = name, hasNameError = false, stepInfo = null) }
    }

    fun onUserImageChange(image: ImageModel) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(userImage = image) }
    }

    fun onEmailChange(email: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(email = email, hasEmailError = false, stepInfo = null) }
    }

    fun onPasswordChange(password: String) {
        if (_uiState.value.actionLoading) return
        _uiState.update { it.copy(password = password, hasPasswordError = false, stepInfo = null) }
    }

}