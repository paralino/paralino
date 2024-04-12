package app.paralino.core.presentation.settings.passwordchange

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
    object PasswordChanged : ChangePasswordScreenEvent()
}
