package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDateTime

sealed interface GroupMemberItemState {
    val memberState: GroupMemberState
    val keyId: String
    val lastPing: String?
}

data class GroupMemberState constructor(
    val groupId: String,
    val memberId: String,
    val userId: String,
    val actionItems: List<MemberAction>,
    val name: String,
    val image: ImageModel?,
    val isCurrentUser: Boolean,
    val isVerified: Boolean
) {
    constructor(member: MemberModel, isCurrentUser: Boolean) : this(
        groupId = member.groupId,
        memberId = member.id,
        userId = member.userId,
        actionItems = emptyList(),
        name = member.name,
        image = member.image,
        isCurrentUser = isCurrentUser,
        isVerified = member.isVerified
    )
}

data class GroupMemberItemStateWithOneLocation constructor(
    override val memberState: GroupMemberState,
    val location: GroupMemberItemLocationState?,
    override val lastPing: String?
) : GroupMemberItemState {
    override val keyId: String
        get() = location?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    val locations: List<GroupMemberItemLocationState>,
    val allowShowingDevices: Boolean,
    override val lastPing: String?
) : GroupMemberItemState {
    override val keyId: String
        get() = memberState.memberId

    val singleLocation: GroupMemberItemLocationState? = locations.takeIf { it.size == 1 }?.first()
    val statusTag: MemberDeviceStatusType
        get() = singleLocation?.status
            ?: locations.firstOrNull { it.status == MemberDeviceStatusType.ACTIVE }?.status
            ?: MemberDeviceStatusType.INACTIVE
}

data class GroupMemberItemLocationState constructor(
    val locationId: String,
    val name: String?,
    val image: ImageModel,
    val batterLevel: Int?,
    val speed: String?,
    val elevation: String?,
    val lastPing: String?,
    val status: MemberDeviceStatusType?,
    val canEdit: Boolean,
    val currentDevice: Boolean
) {
    constructor(
        memberLocation: MemberLocation,
        name: String? = memberLocation.name,
        image: ImageModel?,
        canEdit: Boolean,
        currentDevice: Boolean,
        showSpeedAndElevation: Boolean,
        formatter: IValueFormatter
    ) : this(
        locationId = memberLocation.id,
        name = name,
        image = image ?: ImageModel.emoji(Emojis.lock),
        batterLevel = memberLocation.status.data?.batteryLevel ?: memberLocation.locationData.data?.batteryLevel,
        speed = memberLocation.locationData.data?.speed?.takeIf { showSpeedAndElevation }?.let { formatter.getSpeedSmall(it) },
        elevation = memberLocation.locationData.data?.altitude?.takeIf { showSpeedAndElevation }?.let { formatter.getDistanceSmall(it) },
        lastPing = formatter.getLastPing(memberLocation),
        status = if(memberLocation.isEnded) MemberDeviceStatusType.NOT_SHARING
        else if (memberLocation.isExpired) MemberDeviceStatusType.INACTIVE
        else memberLocation.status.data?.statusType ?: MemberDeviceStatusType.INACTIVE,
        canEdit = canEdit,
        currentDevice = currentDevice
    )
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION, BLOCK_USER, UNBLOCK_USER;
}