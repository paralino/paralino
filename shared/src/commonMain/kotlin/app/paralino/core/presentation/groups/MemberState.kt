package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDateTime

sealed interface GroupMemberItemState {
    val memberState: GroupMemberState
    val keyId: String
}

data class GroupMemberState constructor(
    val groupId: String,
    val memberId: String,
    val userId: String,
    val actionItem: MemberAction? = null,
    val name: String,
    val image: ImageModel?,
    val isCurrentUser: Boolean,
    val isVerified: Boolean
) {
    constructor(member: MemberModel, isCurrentUser: Boolean) : this(
        groupId = member.groupId,
        memberId = member.id,
        userId = member.userId,
        actionItem = null,
        name = member.name,
        image = member.image,
        isCurrentUser = isCurrentUser,
        isVerified = member.isVerified
    )
}

data class GroupMemberItemStateWithOneLocation constructor(
    override val memberState: GroupMemberState,
    val location: GroupMemberItemLocationState?,
) : GroupMemberItemState {
    override val keyId: String
        get() = location?.locationId?.takeIf { it.isNotBlank() } ?: memberState.memberId
    val status: GroupMemberStatus get() = location?.status ?: GroupMemberStatus.INACTIVE
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    val locations: List<GroupMemberItemLocationState>,
) : GroupMemberItemState {
    override val keyId: String
        get() = memberState.memberId
}

data class GroupMemberItemLocationState constructor(
    val locationId: String,
    val name: String?,
    val image: ImageModel,
    val batterLevel: Int?,
    val speedMs: Float?,
    val elevationMeters: Float?,
    val lastPing: LocalDateTime?,
    val status: GroupMemberStatus,
    val canEdit: Boolean,
    val currentDevice: Boolean
) {
    constructor(
        memberLocation: MemberLocation,
        name: String? = memberLocation.name,
        image: ImageModel?,
        canEdit: Boolean,
        currentDevice: Boolean,
        showSpeedAndElevation: Boolean
    ) : this(
        locationId = memberLocation.id,
        name = name,
        image = image ?: ImageModel.emoji(Emojis.lock),
        batterLevel = memberLocation.locationData.data?.batteryLevel,
        speedMs = memberLocation.locationData.data?.speed?.takeIf { showSpeedAndElevation },
        elevationMeters = memberLocation.locationData.data?.altitude?.takeIf { showSpeedAndElevation },
        lastPing = memberLocation.locationData.data?.timeUTC?.isoToLocalDateTime(),
        status = if (memberLocation.isActive) GroupMemberStatus.ACTIVE else GroupMemberStatus.INACTIVE,
        canEdit = canEdit,
        currentDevice = currentDevice
    )
}

enum class GroupMemberStatus {
    ACTIVE, INACTIVE
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION;
}