package app.paralino.core.presentation.login.recover

import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.models.Resource
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class RecoverKeysScreenState constructor(
    val uiStep: RecoverKeysUiStep,
    val logoutLoading: Boolean? = null,
    val checkLoading: Boolean? = null,
    val resetLoading: Boolean? = null,
    val alertDialog: MainAlertDialog? = null
){
    val actionLoading: Boolean get() = logoutLoading == true || resetLoading == true || checkLoading == true
}

sealed interface RecoverKeysUiStep {

    sealed interface Reset: RecoverKeysUiStep {
        data object ResetGuest: Reset
        data class ResetClassicAccount(
            val currentPassword: String = "",
            val hasCurrentPasswordError: Boolean = false,
        ): Reset
    }

    sealed interface Recover: RecoverKeysUiStep {
        data class RecoverGuest(
            val recoverState: Resource<GeneratedUserKeys?>,
        ): Recover

        data class RecoverClassicAccount(
            val recoverState: Resource<GeneratedUserKeys?>,
            val oldPassword: String = "",
            val hasOldPasswordError: Boolean = false,
            val currentPassword: String = "",
            val hasCurrentPasswordError: Boolean = false,
        ): Recover
    }
}

sealed class RecoverKeysScreenEvent {}

sealed class RecoverKeysScreenUiEvent {
    data object HideDialog: RecoverKeysScreenUiEvent()
    data object Logout: RecoverKeysScreenUiEvent()
    data object ConfirmLogout: RecoverKeysScreenUiEvent()
    data object RecoverAccount: RecoverKeysScreenUiEvent()
    data object ConfirmRecoverAccount: RecoverKeysScreenUiEvent()
    data object ResetAccount: RecoverKeysScreenUiEvent()
    data object ConfirmResetAccount: RecoverKeysScreenUiEvent()
    data class EditOldPassword(val password: String): RecoverKeysScreenUiEvent()
    data class EditCurrentPassword(val password: String): RecoverKeysScreenUiEvent()
}