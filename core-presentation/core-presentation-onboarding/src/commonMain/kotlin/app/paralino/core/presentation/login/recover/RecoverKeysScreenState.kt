package app.paralino.core.presentation.login.recover

import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog

data class RecoverKeysScreenState constructor(
    val uiStep: RecoverKeysUiStep,
    val logoutLoading: Boolean? = null,
    val actionLoading: Boolean? = null,
    val alertDialog: MainAlertDialog? = null
) {
    val isInProgress: Boolean = logoutLoading == true || actionLoading == true
}

sealed interface RecoverKeysUiStep {

    // error message when remote keys can't be fetched + retry button
    data object FetchKeysFailed: RecoverKeysUiStep

    // only textual info for reset + confirm reset button
    data object MissingKeysResetOnlyGuest: RecoverKeysUiStep
    data object InvalidKeysResetGuestAccount: RecoverKeysUiStep

    // only textual info for recovery + check keys button
    data object TryRecoverGuestAccount: RecoverKeysUiStep

    // success message for recovery + confirm button
    data class RecoverGuestAccountFinalStep(
        val keys: GeneratedUserKeys
    ): RecoverKeysUiStep

    // info message + old password input + check button
    data class TryRecoverClassicAccount(
        val oldPassword: String = "",
        val hasOldPasswordError: Boolean = false,
        val hasWrongOldPasswordError: Boolean = false
    ): RecoverKeysUiStep

    // info message + current password input + success message for recovery + confirm recovery button
    data class RecoverClassicAccountFinalStep(
        val confirmedOldPassword: String,
        val currentPassword: String,
        val hasCurrentPasswordError: Boolean,
        val hasWrongCurrentPasswordError: Boolean,
        val keys: GeneratedUserKeys,
    ): RecoverKeysUiStep

    // info message + current password input + confirm reset button + skip to recover button
    data class ResetClassicAccount(
        val canRecoverOldKeys: Boolean,
        val currentPassword: String = "",
        val hasCurrentPasswordError: Boolean = false,
        val hasWrongCurrentPasswordError: Boolean = false
    ): RecoverKeysUiStep
}

sealed class RecoverKeysScreenEvent {}

sealed class RecoverKeysScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : RecoverKeysScreenUiEvent()
    data object OnSkipAction: RecoverKeysScreenUiEvent()
    data object OnConfirmAction: RecoverKeysScreenUiEvent()
    data object Logout: RecoverKeysScreenUiEvent()
    data class EditOldPassword(val password: String): RecoverKeysScreenUiEvent()
    data class EditCurrentPassword(val password: String): RecoverKeysScreenUiEvent()
}