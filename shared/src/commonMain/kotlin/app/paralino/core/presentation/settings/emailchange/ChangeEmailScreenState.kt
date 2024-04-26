package app.paralino.core.presentation.settings.emailchange

data class ChangeEmailScreenState(
    val currentEmail: String,
    val email: String = "",
    val password: String = "",
    val hasEmailError: Boolean = false,
    val hasPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
)

sealed class ChangeEmailScreenEvent {
    object EmailChanged : ChangeEmailScreenEvent()
}