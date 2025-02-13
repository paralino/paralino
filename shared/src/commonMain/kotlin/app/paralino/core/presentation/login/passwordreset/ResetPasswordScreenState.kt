package app.paralino.core.presentation.login.passwordreset

import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.common.IFormValidationResult

data class ResetPasswordScreenState constructor(
    val email: String = "",
    val hasEmailError: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val hasNewPasswordError: Boolean = false,
    val hasConfirmPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
) {
    val passwordValidation: FormValidation.PasswordConfirm.PasswordConfirmValidationResult =
        FormValidation.PasswordConfirm.validate(newPassword, confirmPassword)
}

sealed class ResetPasswordScreenEvent {
    data class Error(val exception: Throwable) : ResetPasswordScreenEvent()
    data class ValidationError(val validationResult: IFormValidationResult) :
        ResetPasswordScreenEvent()

    object CredentialsError : ResetPasswordScreenEvent()
    object InvalidToken : ResetPasswordScreenEvent()
    object PasswordChanged : ResetPasswordScreenEvent()
}
