package app.paralino.core.presentation.map

import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class GroupsInitEmptyScreenState(
    val alertDialog: MainAlertDialog?,
    val personalCode: PersonalCodeScreenCodeData?,
    val errors: List<ConditionErrorType>
)

sealed class GroupsInitEmptyScreenUiEvent() {
    data object OpenPersonalCodes: GroupsInitEmptyScreenUiEvent()
    data object CopyPersonalCode: GroupsInitEmptyScreenUiEvent()
    data class SharePersonalCode(val content: String): GroupsInitEmptyScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : GroupsInitEmptyScreenUiEvent()
    data object TryCreateGroup: GroupsInitEmptyScreenUiEvent()
    data class ErrorItemAction(val item: ConditionErrorType): GroupsInitEmptyScreenUiEvent()
}