package app.paralino.core.presentation.settings.emailchange

import app.paralino.core.domain.user.ChangeCredentialsStep

data class ChangeEmailScreenState(
    val currentEmail: String,
    val email: String = "",
    val password: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val credentialsStep: ChangeCredentialsStep? = null,
    val actionLoading: Boolean? = null,
)

sealed class ChangeEmailScreenEvent {
    object EmailChanged : ChangeEmailScreenEvent()
}