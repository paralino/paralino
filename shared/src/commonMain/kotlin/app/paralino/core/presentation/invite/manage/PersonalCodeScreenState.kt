package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.base.ImageModel

data class PersonalCodeScreenState(
    val activeCode: PersonalCodeScreenCodeData?,
    val inactiveCodes: List<PersonalCodeScreenCodeData>,
    val maxCodes: Int,
    val canCreate: Boolean,
    val createLoading: Boolean,
    val actionLoading: Boolean
) {
    val loading get() = createLoading || actionLoading
}

data class PersonalCodeScreenCodeData(
    val id: String,
    val code: String,
    val userImage: ImageModel,
    val userName: String,
    val canDelete: Boolean,
    val isValid: Boolean,
)

sealed class PersonalCodeScreenEvent() {
    data class Error(val error: Throwable) : PersonalCodeScreenEvent()
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}