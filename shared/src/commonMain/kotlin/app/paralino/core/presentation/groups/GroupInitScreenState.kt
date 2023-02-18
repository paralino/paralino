package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis

data class GroupInitScreenState constructor(
    val userImage: ImageModel = ImageModel.emoji(Emojis.robot),
    val personalCode: PersonalCodeScreenCodeData? = null,
    val errors: List<ConditionErrorType> = emptyList(),
)

sealed class GroupInitScreenEvent() {
    data class Error(val error: Throwable) : GroupInitScreenEvent()
    object EmailVerificationSent : GroupInitScreenEvent()
}