package app.paralino.core.presentation.paralinoid

import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class PersonalCodeScreenState(
    val activeCode: PersonalCodeScreenCodeData?,
    val inactiveCodes: List<PersonalCodeScreenCodeData>,
    val maxCodes: Int,
    val canCreateAlternativeId: Boolean,
    val createLoading: Boolean,
    val alertDialog: MainAlertDialog?,
    val errors: List<ConditionErrorType> = emptyList()
) {
    val loading get() = createLoading || inactiveCodes.any { it.isActionLoading }
}

sealed class PersonalCodeScreenEvent() {
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}

sealed class PersonalCodeScreenUiEvent() {
    data object HideDialog: PersonalCodeScreenUiEvent()
    data object CreateCode: PersonalCodeScreenUiEvent()
    data object DisableCode: PersonalCodeScreenUiEvent()
    data object DisableCodeConfirm: PersonalCodeScreenUiEvent()
    data class ActivateCode(val code: String): PersonalCodeScreenUiEvent()
    data object CopyPersonalCode: PersonalCodeScreenUiEvent()
}