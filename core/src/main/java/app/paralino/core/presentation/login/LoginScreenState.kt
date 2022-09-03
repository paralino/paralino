package app.paralino.core.presentation.login

import app.paralino.core.presentation.common.FormValidationResult

data class LoginScreenState constructor(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val hasPasswordConfirmError: Boolean = false,
    val uiType: LoginScreenUiType = defaultStartScreen,
    val actionLoading: Boolean = false,
    val stepInfo: SignUpStepInfo? = null,
) {
    companion object {
        val defaultStartScreen = LoginScreenUiType.SIGNUP
    }
}

enum class LoginScreenUiType {
    LOGIN, SIGNUP, SIGNUP_GUEST, FORGOT_PASSWORD;

    val isCreatingAccount: Boolean get() = this == SIGNUP || this == SIGNUP_GUEST
}

sealed class LoginScreenEvent {
    data class Error(val exception: Throwable) : LoginScreenEvent()
    data class LoginError(val errorType: SignUpErrorType) : LoginScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) : LoginScreenEvent()
    object UserReady : LoginScreenEvent()
    object PasswordResetDone : LoginScreenEvent()
}


sealed interface SignUpStepInfo {
    enum class Info : SignUpStepInfo {
        START_LOGIN, START_SIGNUP, START_PASSWORD_RESET, CRYPTO_SETUP, USER_SETUP, DONE
    }

    enum class Error : SignUpStepInfo {
        LOGIN, SIGN_UP, PASSWORD_RESET, CRYPTO, USER_DATA
    }
}

sealed class SignUpErrorType {
    object ACCOUNT_CONFLICT : SignUpErrorType()
    object CREDENTIALS : SignUpErrorType()
    object LOGIN_CRYPTO : SignUpErrorType()
    object SIGNUP_CRYPTO : SignUpErrorType()
    object SIGNUP_DATA : SignUpErrorType()
    data class SOURCE(val error: Throwable) : SignUpErrorType()
}

enum class SignUpStep {
    STARTING,
    GENERATING_KEYS,
    INIT_USER_DATA,
    SAVE_USER,
    ALL_DONE
}

class SignUpStepException constructor(val step: SignUpStep, cause: Throwable) : Exception(cause)