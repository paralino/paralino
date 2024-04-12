package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.base.ImageModel

data class PersonalCodeScreenState(
    val activeCode: PersonalCodeScreenCodeData?,
    val inactiveCodes: List<PersonalCodeScreenCodeData>,
    val maxCodes: Int,
    val canCreateAlternativeId: Boolean,
    val createLoading: Boolean,
) {
    val loading get() = createLoading || inactiveCodes.any { it.isActionLoading }
}

data class PersonalCodeScreenCodeData(
    val id: String,
    val code: String,
    val userImage: ImageModel,
    val userName: String,
    val canDisable: Boolean,
    val isValid: Boolean,
    val isActionLoading: Boolean
)

sealed class PersonalCodeScreenEvent() {
    data class Error(val error: Throwable) : PersonalCodeScreenEvent()
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}