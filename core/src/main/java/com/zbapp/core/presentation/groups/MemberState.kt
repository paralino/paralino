package com.zbapp.core.presentation.groups

import com.zbapp.core.models.base.ImageModel
import kotlinx.datetime.LocalDateTime

data class GroupMemberItemState constructor(
    val actionItem: MemberAction? = null,
    val name: String,
    val image: ImageModel?,
    val batterLevel: Int?,
    val lastPing: LocalDateTime?,
    val status: GroupMemberStatus
)

enum class GroupMemberStatus {
    ACTIVE, INACTIVE
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION;
}