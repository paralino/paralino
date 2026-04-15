package app.paralino.core.presentation.paralinoid

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.utils.Emojis

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
                    userImage = it.codeData.data?.ownerImage ?: ImageModel.Companion.emoji(Emojis.robot),
                    userName = it.codeData.data?.ownerName ?: "---",
                    canDisable = false,
                    isValid = true,
                    isActionLoading = false
                )
            }
        }
    }
}