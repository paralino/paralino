package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

data class GroupInitScreenState constructor(
    val userEmail: String? = null,
    val userImage: ImageModel = ImageModel.emoji(Emojis.robot),
    val personalCode: PersonalCodeScreenCodeData? = null,
    val errors: List<ConditionErrorType> = emptyList(),
    val alertDialog: MainAlertDialog? = null
)

sealed class GroupInitScreenEvent()

sealed class GroupInitScreenUiEvent() {
    data object CopyPersonalCode: GroupInitScreenUiEvent()
    data object HideDialog: GroupInitScreenUiEvent()
    data object SendEmailVerification: GroupInitScreenUiEvent()
    data object ConfirmSendEmailVerification: GroupInitScreenUiEvent()
}