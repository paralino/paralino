package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class PersonalCodeScreenState(
    val activeCode: PersonalCodeScreenCodeData?,
    val inactiveCodes: List<PersonalCodeScreenCodeData>,
    val maxCodes: Int,
    val canCreateAlternativeId: Boolean,
    val createLoading: Boolean,
    val alertDialog: MainAlertDialog?
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
    object CodeCreated : PersonalCodeScreenEvent()
    object CodeDeleted : PersonalCodeScreenEvent()
}

sealed class PersonalCodeScreenUiEvent() {
    data object HideDialog: PersonalCodeScreenUiEvent()
    data object CreateCode: PersonalCodeScreenUiEvent()
    data object DisableCode: PersonalCodeScreenUiEvent()
    data object DisableCodeConfirm: PersonalCodeScreenUiEvent()
    data class ActivateCode(val code: String): PersonalCodeScreenUiEvent()
    data class CopyToClipboard(val content: String): PersonalCodeScreenUiEvent()
}