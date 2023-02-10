package app.paralino.core.presentation.invite.manage

data class PersonalCodeScreenState(
    val code: PersonalCodeScreenCodeData?,
    val createLoading: Boolean,
    val actionLoading: Boolean
)

data class PersonalCodeScreenCodeData(
    val id: String,
    val code: String,
    val validityUTC: String?
)

sealed class PersonalCodeScreenEvent() {
    data class Error(val error: Throwable) : PersonalCodeScreenEvent()
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}