package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberModel
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDateTime

data class GroupMemberItemState constructor(
    val id: String,
    val userId: String,
    val actionItem: MemberAction? = null,
    val name: String,
    val image: ImageModel?,
    val batterLevel: Int?,
    val speedMs: Float?,
    val elevationMeters: Float?,
    val lastPing: LocalDateTime?,
    val status: GroupMemberStatus
) {
    constructor(member: MemberModel, showSpeedAndElevation: Boolean = false): this(
        id = member.id,
        userId = member.userId,
        actionItem = null,
        name = member.name,
        image = member.image,
        batterLevel = member.mutableData?.batteryLevel,
        speedMs = member.mutableData?.speed?.takeIf { showSpeedAndElevation },
        elevationMeters = member.mutableData?.altitude?.takeIf { showSpeedAndElevation },
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