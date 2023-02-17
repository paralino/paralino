package app.paralino.core.presentation.invite.input

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.utils.Emojis

sealed class MemberInviteScreenState() {

    abstract val code: String
    abstract val hasInputError: Boolean
    abstract val actionLoading: Boolean

    data class Empty constructor(
        override val code: String = "",
        override val hasInputError: Boolean = false,
        override val actionLoading: Boolean = false,
    ) : MemberInviteScreenState()

    data class Loaded constructor(
        val codeReadData: UserPersonalCodeData,
        val group: GroupItemState?,
        val canChangeGroup: Boolean,
        override val hasInputError: Boolean,
        override val actionLoading: Boolean,
    ) : MemberInviteScreenState() {

        override val code: String get() = codeReadData.formattedCode
        val userId: String get() = codeReadData.ownerId
        val userName: String get() = codeReadData.ownerName
        val userImage: ImageModel
            get() = codeReadData.ownerImage ?: ImageModel.emoji(Emojis.robot)
    }
}

sealed class MemberInviteScreenEvent() {
    data class Error(val error: Throwable) : MemberInviteScreenEvent()
    object ErrorWrongCode : MemberInviteScreenEvent()
    object ErrorMissingGroup : MemberInviteScreenEvent()
    object MemberAdded : MemberInviteScreenEvent()
}
