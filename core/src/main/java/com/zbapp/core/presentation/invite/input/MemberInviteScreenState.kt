package com.zbapp.core.presentation.invite.input

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.base.asImageModel
import com.zbapp.core.models.raw.PersonalCodeReadData
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.utils.Emojis

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
        val codeReadData: PersonalCodeReadData,
        val group: GroupItemData?,
        val canChangeGroup: Boolean,
        override val hasInputError: Boolean,
        override val actionLoading: Boolean,
    ) : MemberInviteScreenState() {

        override val code: String get() = codeReadData.code
        val userId: String get() = codeReadData.ownerId
        val userName: String get() = codeReadData.ownerName
        val userImage: ImageModel
            get() = codeReadData.ownerImageBase64?.asImageModel()
                ?: ImageModel.emoji(Emojis.robot)
    }
}

sealed class MemberInviteScreenEvent() {
    data class Error(val error: Throwable) : MemberInviteScreenEvent()
    object ErrorMissingGroup : MemberInviteScreenEvent()
    object MemberAdded : MemberInviteScreenEvent()
}
