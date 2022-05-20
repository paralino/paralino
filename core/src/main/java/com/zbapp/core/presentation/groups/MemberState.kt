package com.zbapp.core.presentation.groups

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDateTime
import javax.sound.midi.MetaMessage

data class GroupMemberItemState constructor(
    val id: String,
    val actionItem: MemberAction? = null,
    val name: String,
    val image: ImageModel?,
    val batterLevel: Int?,
    val lastPing: LocalDateTime?,
    val status: GroupMemberStatus
) {
    constructor(member: MemberModel): this(
        id = member.id,
        actionItem = null,
        name = member.name,
        image = member.image,
        batterLevel = member.mutableData?.batteryLevel,
        lastPing = member.mutableData?.timeUTC?.isoToLocalDateTime(),
        status = if(member.isActive) GroupMemberStatus.ACTIVE else GroupMemberStatus.INACTIVE
    )
}

enum class GroupMemberStatus {
    ACTIVE, INACTIVE
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION;
}