package app.paralino.core.presentation.settings.convertaccount

import app.paralino.core.presentation.common.FormValidation

data class ConvertAccountScreenState constructor(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val hasPasswordConfirmError: Boolean = false,
    val actionLoading: Boolean? = null,
) {
    val passwordValidation: FormValidation.PasswordConfirm.PasswordConfirmValidationResult =
        FormValidation.PasswordConfirm.validate(password, passwordConfirm)
}

sealed class ConvertAccountScreenEvent {
    object AccountUpdated : ConvertAccountScreenEvent()
}
