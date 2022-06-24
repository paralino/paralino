package com.zbapp.core.presentation.login.recover

import com.zbapp.core.models.Resource
import com.zbapp.core.presentation.common.FormValidationResult

data class RecoverKeysScreenState constructor(
    val canRecoverOldKey: Boolean = false,
    val recoverState: Resource<Boolean> = Resource.NotInitialized,
    val oldPassword: String = "",
    val hasOldPasswordError: Boolean = false,
    val actionLoading: Boolean? = null,
)


sealed class RecoverKeysScreenEvent {
    data class Error(val exception: Throwable) : RecoverKeysScreenEvent()
    data class ValidationError(val validationResult: FormValidationResult) :
        RecoverKeysScreenEvent()

    object CredentialsError : RecoverKeysScreenEvent()
    object PasswordChanged : RecoverKeysScreenEvent()
}