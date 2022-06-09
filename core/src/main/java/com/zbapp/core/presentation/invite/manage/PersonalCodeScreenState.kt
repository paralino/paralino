package com.zbapp.core.presentation.invite.manage

import com.zbapp.core.models.user.UserPersonalCode

data class PersonalCodeScreenState(
    val code: UserPersonalCode?,
    val createLoading: Boolean,
    val actionLoading: Boolean
)

sealed class PersonalCodeScreenEvent() {
    data class Error(val error: Throwable) : PersonalCodeScreenEvent()
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}