package app.paralino.core.presentation.login.recover

import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.models.Resource
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class RecoverKeysScreenState constructor(
    val canRecoverOldKey: Boolean = false,
    val recoverState: Resource<GeneratedUserKeys?> = Resource.NotInitialized,
    val masterAuthKey: String = "",
    val oldPassword: String = "",
    val hasOldPasswordError: Boolean = false,
    val logoutLoading: Boolean? = null,
    val checkLoading: Boolean? = null,
    val resetLoading: Boolean? = null,
    val alertDialog: MainAlertDialog? = null
){
    val actionLoading: Boolean get() = logoutLoading == true || resetLoading == true || checkLoading == true
}


sealed class RecoverKeysScreenEvent {}

sealed class RecoverKeysScreenUiEvent {
    data object HideDialog: RecoverKeysScreenUiEvent()
    data object Logout: RecoverKeysScreenUiEvent()
    data object ConfirmLogout: RecoverKeysScreenUiEvent()
    data object RecoverAccount: RecoverKeysScreenUiEvent()
    data object ConfirmRecoverAccount: RecoverKeysScreenUiEvent()
    data object CheckEncryptionKey: RecoverKeysScreenUiEvent()
    data object ResetAccount: RecoverKeysScreenUiEvent()
    data object ConfirmResetAccount: RecoverKeysScreenUiEvent()
    data class EditOldPassword(val password: String): RecoverKeysScreenUiEvent()
}