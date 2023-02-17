package app.paralino.core.presentation.groups

import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.utils.ConditionErrorType

data class GroupInitScreenState constructor(
    val personalCode: PersonalCodeScreenCodeData? = null,
    val errors: List<ConditionErrorType> = emptyList(),
)

sealed class GroupInitScreenEvent() {
    data class Error(val error: Throwable) : GroupInitScreenEvent()
    object EmailVerificationSent : GroupInitScreenEvent()
}