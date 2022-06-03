package com.zbapp.core.presentation.invite.manage

import com.zbapp.core.models.invite.PersonalCodeData

data class PersonalCodeScreenState(
    val code: PersonalCodeData?,
    val createLoading: Boolean,
    val actionLoading: Boolean
)

sealed class PersonalCodeScreenEvent() {
    data class Error(val error: Throwable) : PersonalCodeScreenEvent()
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}