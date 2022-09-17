package app.paralino.core.presentation.login.recover

import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.models.Resource
import app.paralino.core.presentation.common.FormValidationResult

data class RecoverKeysScreenState constructor(
    val canRecoverOldKey: Boolean = false,
    val recoverState: Resource<GeneratedUserKeys?> = Resource.NotInitialized,
    val backupPassword: String = "",
    val oldPassword: String = "",
    val hasOldPasswordError: Boolean = false,
    val logoutLoading: Boolean? = null,
    val checkLoading: Boolean? = null,
    val resetLoading: Boolean? = null,
){
    val actionLoading: Boolean get() = logoutLoading == true || resetLoading == true || checkLoading == true
}


sealed class RecoverKeysScreenEvent {
    data class Error(val exception: Throwable) : RecoverKeysScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        RecoverKeysScreenEvent()

    object CredentialsError : RecoverKeysScreenEvent()
}