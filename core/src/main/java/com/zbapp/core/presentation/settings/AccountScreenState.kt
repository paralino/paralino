package com.zbapp.core.presentation.settings

import com.zbapp.core.models.Payload
import com.zbapp.core.models.base.ImageModel

data class AccountScreenState constructor(
    val userState: UserState,
    val logoutState: Payload<Unit>? = null,
    val deleteAccountState: Payload<Unit>? = null,
)

data class UserState constructor(
    val name: String,
    val userId: String,
    val email: String,
    val image: ImageModel?
)

sealed class AccountScreenEvent() {
    data class Error(val error: Throwable): AccountScreenEvent()
}