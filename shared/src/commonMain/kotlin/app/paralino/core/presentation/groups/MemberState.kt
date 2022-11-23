package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDateTime

data class GroupMemberItemState constructor(
    val locationId: String,
    val memberId: String,
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
    // TODO refactor to use separate devices
    constructor(member: MemberModel, memberLocation: MemberLocation?, showSpeedAndElevation: Boolean = false): this(
        locationId = memberLocation?.id ?: "",
        memberId = member.id,
        userId = member.userId,
        actionItem = null,
        name = arrayOf(member.name, memberLocation?.deviceData?.data?.deviceName).filterNotNull().filter { it.isNotBlank() }.joinToString(" - "),
        image = member.image,
        batterLevel = memberLocation?.locationData?.data?.batteryLevel,
        speedMs = memberLocation?.locationData?.data?.speed?.takeIf { showSpeedAndElevation },
        elevationMeters = memberLocation?.locationData?.data?.altitude?.takeIf { showSpeedAndElevation },
        lastPing = memberLocation?.locationData?.data?.timeUTC?.isoToLocalDateTime(),
        status = if(memberLocation?.isActive == true) GroupMemberStatus.ACTIVE else GroupMemberStatus.INACTIVE
    )
}

enum class GroupMemberStatus {
    ACTIVE, INACTIVE
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION;
}