package app.paralino.core.presentation.login

import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class LoginScreenState constructor(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val hasPasswordConfirmError: Boolean = false,
    val canUseEmail: Boolean = false,
    val uiType: LoginScreenUiType,
    val actionLoading: Boolean = false,
    val stepInfo: SignUpStepInfo? = null,
    val alertDialog: MainAlertDialog? = null
) {
    val passwordValidation: FormValidation.PasswordConfirm.PasswordConfirmValidationResult =
        FormValidation.PasswordConfirm.validate(password, passwordConfirm)

    companion object {
        val defaultStartScreen = LoginScreenUiType.SIGNUP_GUEST
    }
}

data class LoggedOutScreenState(
    val hasLogin: Boolean
)

enum class LoginScreenUiType {
    LOGIN, SIGNUP, SIGNUP_GUEST, FORGOT_PASSWORD;

    val isCreatingAccount: Boolean get() = this == SIGNUP || this == SIGNUP_GUEST
}

sealed class LoginScreenEvent {
    object UserReady : LoginScreenEvent()
    object PasswordResetDone : LoginScreenEvent()
}

sealed class LoginScreenUiEvent {
    data object LoginOrSignup : LoginScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : LoginScreenUiEvent()
}

sealed interface SignUpStepInfo {
    enum class Info : SignUpStepInfo {
        START_LOGIN, START_SIGNUP, START_PASSWORD_RESET, CRYPTO_SETUP, USER_SETUP, DONE
    }

    enum class Error : SignUpStepInfo {
        LOGIN, SIGN_UP, PASSWORD_RESET, CRYPTO, USER_DATA, RECOVER_GUEST
    }
}

sealed class SignUpErrorType {
    object ACCOUNT_CONFLICT : SignUpErrorType()
    object CREDENTIALS : SignUpErrorType()
    object CREDENTIALS_GUEST : SignUpErrorType()
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