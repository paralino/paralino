package app.paralino.core.presentation.invite.manage

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

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

data class PersonalCodeScreenCodeData(
    val id: String,
    val code: String,
    val userImage: ImageModel,
    val userName: String,
    val canDisable: Boolean,
    val isValid: Boolean,
    val isActionLoading: Boolean
) {
    companion object {
        fun viewOnlyCode(code: UserPersonalCode?): PersonalCodeScreenCodeData? {
            return code?.let {
                PersonalCodeScreenCodeData(
                    id = code.id,
                    code = it.formattedCode,
                    userImage = it.codeData.data?.ownerImage ?: ImageModel.emoji(Emojis.robot),
                    userName = it.codeData.data?.ownerName ?: "---",
                    canDisable = false,
                    isValid = true,
                    isActionLoading = false
                )
            }
        }
    }
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