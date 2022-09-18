package app.paralino.core.presentation.settings.emailchange

import app.paralino.core.presentation.common.FormValidationResult

data class ChangeEmailScreenState(
    val currentEmail: String,
    val email: String = "",
    val password: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
)

sealed class ChangeEmailScreenEvent {
    data class Error(val exception: Throwable) : ChangeEmailScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) : ChangeEmailScreenEvent()

    object EmailError : ChangeEmailScreenEvent()
    object CredentialsError : ChangeEmailScreenEvent()
    object EmailChanged : ChangeEmailScreenEvent()
}