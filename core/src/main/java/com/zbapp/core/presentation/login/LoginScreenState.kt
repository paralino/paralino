package com.zbapp.core.presentation.login

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.common.FormValidationResult
import com.zbapp.core.utils.Emojis

data class LoginScreenState(
    val email: String = "",
    val password: String = "",
    val userName: String = "",
    val userImage: ImageModel = ImageModel.emoji(Emojis.robot),
    val hasNameError: Boolean = false,
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val uiType: LoginScreenUiType = LoginScreenUiType.LOGIN,
    val actionLoading: Boolean = false,
    val stepInfo: SignUpStepInfo? = null,
)

enum class LoginScreenUiType {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}

sealed class LoginScreenEvent {
    data class Error(val exception: Throwable) : LoginScreenEvent()
    data class LoginError(val errorType: SignUpErrorType) : LoginScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) : LoginScreenEvent()
    object UserReady : LoginScreenEvent()
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