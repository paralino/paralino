package com.zbapp.core.presentation.login

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.common.FormValidationResult
import com.zbapp.core.repo.user.SignUpStep
import com.zbapp.core.repo.user.SignUpStepException
import com.zbapp.core.utils.Emojis

data class LoginScreenState(
    val email: String = "",
    val password: String = "",
    val userName: String = "",
    val userImage: ImageModel = ImageModel.emoji(Emojis.robot),
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val uiType: LoginScreenUiType = LoginScreenUiType.LOGIN,
    val actionLoading: Boolean = false,
    val signUpStep: SignUpStep? = null,
)

enum class LoginScreenUiType {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}

sealed class LoginScreenEvent {
    data class Error(val exception: Throwable): LoginScreenEvent()
    data class LoginError(val error: StepErrorType.LoginErrorType): LoginScreenEvent()
    data class SignUpError(val error: StepErrorType.SignUpErrorType): LoginScreenEvent()
    data class ForgotPasswordError(val exception: SignUpStepException): LoginScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult): LoginScreenEvent()
    object UserReady: LoginScreenEvent()
}


sealed class StepErrorType {
    sealed class LoginErrorType: StepErrorType() {
        object CREDENTIALS: LoginErrorType()
        object CRYPTO_KEYS: LoginErrorType()
        object USER_DATA_INIT: LoginErrorType()
        object USER_DATA_SAVE: LoginErrorType()
        object GENERIC: LoginErrorType()
        data class SOURCE(val error: Throwable): LoginErrorType()
    }

    sealed class SignUpErrorType: StepErrorType() {
        object CREDENTIALS: SignUpErrorType()
        object CRYPTO_KEYS: SignUpErrorType()
        object USER_DATA_INIT: SignUpErrorType()
        object USER_DATA_SAVE: SignUpErrorType()
        object GENERIC: SignUpErrorType()
        data class SOURCE(val error: Throwable): SignUpErrorType()
    }
}