package com.zbapp.core.presentation.login.passwordreset

import com.zbapp.core.presentation.common.FormValidationResult

data class ResetPasswordScreenState constructor(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val hasNewPasswordError: Boolean = false,
    val hasConfirmPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
)

sealed class ResetPasswordScreenEvent {
    data class Error(val exception: Throwable) : ResetPasswordScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        ResetPasswordScreenEvent()

    object CredentialsError : ResetPasswordScreenEvent()
    object PasswordChanged : ResetPasswordScreenEvent()
}
