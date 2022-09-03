package app.paralino.core.presentation.settings.convertaccount

import app.paralino.core.presentation.common.FormValidationResult

data class ConvertAccountScreenState constructor(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val hasPasswordConfirmError: Boolean = false,
    val actionLoading: Boolean? = null,
)

sealed class ConvertAccountScreenEvent {
    data class Error(val exception: Throwable) : ConvertAccountScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        ConvertAccountScreenEvent()

    object EmailError : ConvertAccountScreenEvent()
    object CredentialsError : ConvertAccountScreenEvent()
    object AccountUpdated : ConvertAccountScreenEvent()
}
