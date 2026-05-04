package app.paralino.core.presentation.login.password_confirmation

import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog

data class PasswordConfirmationScreenState(
    val password: String = "",
    val hasPasswordError: Boolean = false,
    val confirmLoading: Boolean? = null,
    val logoutLoading: Boolean? = null,
    val alertDialog: MainAlertDialog? = null
) {
    val actionLoading: Boolean get() = confirmLoading == true || logoutLoading == true
}

sealed class PasswordConfirmationScreenEvent

sealed class PasswordConfirmationScreenUiEvent {
    data object Logout : PasswordConfirmationScreenUiEvent()
    data object ConfirmPassword : PasswordConfirmationScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : PasswordConfirmationScreenUiEvent()
    data class EditPassword(val password: String) : PasswordConfirmationScreenUiEvent()
}
