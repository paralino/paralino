package app.paralino.core.presentation.settings.convertaccount

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
    object AccountUpdated : ConvertAccountScreenEvent()
}
