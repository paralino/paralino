package com.zbapp.core.presentation.settings.passwordchange

import com.zbapp.core.presentation.common.FormValidationResult

data class ChangePasswordScreenState constructor(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val hasOldPasswordError: Boolean = false,
    val hasNewPasswordError: Boolean = false,
    val hasConfirmPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
)

sealed class ChangePasswordScreenEvent {
    data class Error(val exception: Throwable) : ChangePasswordScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        ChangePasswordScreenEvent()

    object RemotePasswordError : ChangePasswordScreenEvent()
    object CredentialsError : ChangePasswordScreenEvent()
    object PasswordChanged : ChangePasswordScreenEvent()
}
