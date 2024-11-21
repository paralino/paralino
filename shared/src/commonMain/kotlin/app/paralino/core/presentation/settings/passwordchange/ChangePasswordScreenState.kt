package app.paralino.core.presentation.settings.passwordchange

import app.paralino.core.presentation.common.FormValidation

data class ChangePasswordScreenState constructor(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val hasOldPasswordError: Boolean = false,
    val hasNewPasswordError: Boolean = false,
    val hasConfirmPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
) {
    val passwordValidation: FormValidation.PasswordConfirm.PasswordConfirmValidationResult =
        FormValidation.PasswordConfirm.validate(newPassword, confirmPassword)
}

sealed class ChangePasswordScreenEvent {
    object PasswordChanged : ChangePasswordScreenEvent()
}
