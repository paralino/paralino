package app.paralino.core.presentation.login.emailverify

import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class VerifyEmailScreenState(
    val currentEmail: String,
    val emailVerificationLink: String = "",
    val hasEmailVerificationLinkError: Boolean = false,
    val isManualEmailVerificationShown: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
    val fullLoading: Boolean = false,
    val actionLoading: Boolean? = null,
) {
    val isLoadingAny: Boolean = fullLoading || actionLoading == true
}

sealed interface VerifyEmailScreenUiEvent {
    data object Logout: VerifyEmailScreenUiEvent
    data object ResendVerification: VerifyEmailScreenUiEvent
    data object ToggleManualVerificationInput: VerifyEmailScreenUiEvent
    data object CheckVerification: VerifyEmailScreenUiEvent
    data object SkipVerification: VerifyEmailScreenUiEvent
    data object EditCurrentEmail: VerifyEmailScreenUiEvent
    data class EditEmailVerification(val verification: String): VerifyEmailScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : VerifyEmailScreenUiEvent
}

sealed interface VerifyEmailScreenEvent {
    data object EditEmail : VerifyEmailScreenEvent
}