package com.zbapp.core.presentation.login

import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.common.FormValidation
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.SignUpStep
import com.zbapp.core.repo.user.SignUpStepException
import com.zbapp.core.utils.exceptions.ConflictException
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
                hasPasswordError = passwordValidation.result,
                hasEmailError = emailValidation.result
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

        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.Password.validate(password)

        _uiState.update {
            it.copy(
                hasPasswordError = passwordValidation.result,
                hasEmailError = emailValidation.result
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
            loginManager.signUp(userName = "", email = email, password = password)
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
                hasEmailError = emailValidation.result
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
                signUpStep = step
            )
        }
        if (step == SignUpStep.ALL_DONE) {
            _eventChannel.send(LoginScreenEvent.Success)
        }
    }

    private suspend fun processSignUpError(error: Throwable) {
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> {
                val errorType = if (error is SignUpStepException) {
                    if (error.cause is UnauthorizedException) {
                        StepErrorType.LoginErrorType.CREDENTIALS
                    } else {
                        when (error.step) {
                            SignUpStep.STARTING -> StepErrorType.LoginErrorType.GENERIC
                            SignUpStep.GENERATING_KEYS -> StepErrorType.LoginErrorType.CRYPTO_KEYS
                            SignUpStep.INIT_USER_DATA -> StepErrorType.LoginErrorType.USER_DATA_INIT
                            SignUpStep.SAVE_USER -> StepErrorType.LoginErrorType.USER_DATA_SAVE
                            SignUpStep.ALL_DONE -> null
                        }
                    }
                } else {
                    StepErrorType.LoginErrorType.SOURCE(error)
                }
                errorType?.let {
                    _eventChannel.send(LoginScreenEvent.LoginError(it))
                }
            }
            LoginScreenUiType.SIGNUP -> {
                val errorType = if (error is SignUpStepException) {
                    if (error.cause is ConflictException) {
                        StepErrorType.SignUpErrorType.CREDENTIALS
                    } else {
                        when (error.step) {
                            SignUpStep.STARTING -> StepErrorType.SignUpErrorType.GENERIC
                            SignUpStep.GENERATING_KEYS -> StepErrorType.SignUpErrorType.CRYPTO_KEYS
                            SignUpStep.INIT_USER_DATA -> StepErrorType.SignUpErrorType.USER_DATA_INIT
                            SignUpStep.SAVE_USER -> StepErrorType.SignUpErrorType.USER_DATA_SAVE
                            SignUpStep.ALL_DONE -> null
                        }
                    }
                } else {
                    StepErrorType.SignUpErrorType.SOURCE(error)
                }
                errorType?.let {
                    _eventChannel.send(LoginScreenEvent.SignUpError(it))
                }
            }
            LoginScreenUiType.FORGOT_PASSWORD -> {
                // TODO add password reset errors
            }
        }
    }

    private fun resetValidationState() {
        _uiState.update {
            it.copy(hasEmailError = false, hasPasswordError = false)
        }
    }

    fun toggleLoginSignup() {
        if (_uiState.value.actionLoading)
            return

        resetValidationState()
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> _uiState.update { it.copy(uiType = LoginScreenUiType.SIGNUP) }
            LoginScreenUiType.SIGNUP -> _uiState.update { it.copy(uiType = LoginScreenUiType.LOGIN) }
            LoginScreenUiType.FORGOT_PASSWORD -> _uiState.update { it.copy(uiType = LoginScreenUiType.LOGIN) }
        }
    }

    fun toggleForgotPassword() {
        if (_uiState.value.actionLoading)
            return

        resetValidationState()
        when (_uiState.value.uiType) {
            LoginScreenUiType.LOGIN -> _uiState.update {
                it.copy(uiType = LoginScreenUiType.FORGOT_PASSWORD,
                    password = "")
            }
            LoginScreenUiType.FORGOT_PASSWORD -> _uiState.update { it.copy(uiType = LoginScreenUiType.LOGIN) }
            else -> { /* do nothing */
            }
        }
    }

}