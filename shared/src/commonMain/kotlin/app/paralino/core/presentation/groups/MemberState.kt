package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.Emojis

sealed interface GroupMemberItemState {
    val memberState: GroupMemberState
    val keyId: String
    val lastPing: String?
    val actionItems: List<MemberAction>
    val singleLocation: GroupMemberItemLocationState?
    val singleValidLocation: GroupMemberItemLocationState?
}

data class GroupMemberState constructor(
    val groupId: String,
    val memberId: String,
    val userId: String,
    val name: String,
    val image: ImageModel?,
    val isCurrentUser: Boolean,
    val isVerified: Boolean,
    val canHaveExpandButton: Boolean
) {
    constructor(
        member: MemberModel,
        isCurrentUser: Boolean,
        canHaveExpandButton: Boolean
    ) : this(
        groupId = member.groupId,
        memberId = member.id,
        userId = member.userId,
        name = member.name,
        image = member.image,
        isCurrentUser = isCurrentUser,
        isVerified = member.isVerified,
        canHaveExpandButton = canHaveExpandButton
    )
}

data class GroupMemberItemStateWithOneLocation constructor(
    override val memberState: GroupMemberState,
    val location: GroupMemberItemLocationState?,
    override val actionItems: List<MemberAction>,
    override val lastPing: String?
) : GroupMemberItemState {
    override val keyId: String
        get() = location?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation: GroupMemberItemLocationState?
        get() = location
    override val singleValidLocation: GroupMemberItemLocationState?
        get() = location?.takeIf { it.isShownAsOldLocation != false }
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    val locations: List<GroupMemberItemLocationState>,
    override val actionItems: List<MemberAction>,
    val allowShowingDevices: Boolean,
    override val lastPing: String?
) : GroupMemberItemState {

    override val keyId: String
        get() = singleLocation?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation = locations.takeIf { it.size == 1 }?.first()
    override val singleValidLocation = singleLocation?.takeIf { it.isShownAsOldLocation != false }

    val filteredActionItems = actionItems.filter {
        if (it == MemberAction.DELETE_LOCATION) singleValidLocation != null
        else true
    }

    val filteredLastPing: String? = lastPing?.takeIf { singleValidLocation != null }

    val statusTag: MemberDeviceStatusType?
        get() = singleValidLocation?.status
            ?: locations.firstOrNull { it.status == MemberDeviceStatusType.ACTIVE }?.status
}

data class GroupMemberItemLocationState constructor(
    val locationId: String,
    val name: String?,
    val image: ImageModel,
    val batterLevel: Int?,
    val isBatteryCharging: Boolean,
    val speed: String?,
    val elevation: String?,
    val lastPing: String?,
    val status: MemberDeviceStatusType?,
    val networkType: NetworkType? = null,
    val signalStrength: NetworkSignalStrength? = null,
    val canEdit: Boolean,
    val currentDevice: Boolean,
    val isShownAsOldLocation: Boolean? = null
) {
    constructor(
        memberLocation: MemberLocation,
        name: String? = memberLocation.name,
        image: ImageModel?,
        canEdit: Boolean,
        currentDevice: Boolean,
        showSpeedAndElevation: Boolean,
        isShownAsOldLocation: Boolean? = null,
        formatter: IValueFormatter
    ) : this(
        locationId = memberLocation.id,
        name = name,
        image = image ?: ImageModel.emoji(Emojis.lock),
        batterLevel = memberLocation.status.data?.validBatteryLevel,
        isBatteryCharging = memberLocation.status.data?.isCharging ?: false,
        speed = memberLocation.locationData.data?.speed?.takeIf { showSpeedAndElevation }?.let { formatter.getSpeedSmall(it) },
        elevation = memberLocation.locationData.data?.altitude?.takeIf { showSpeedAndElevation }?.let { formatter.getDistanceSmall(it) },
        lastPing = formatter.getLastPing(memberLocation),
        status = memberLocation.locationAwareStatusType,
        networkType = memberLocation.status.data?.networkType?.takeIf { showSpeedAndElevation },
        signalStrength = memberLocation.status.data?.signalStrength?.takeIf { showSpeedAndElevation },
        canEdit = canEdit,
        currentDevice = currentDevice,
        isShownAsOldLocation = isShownAsOldLocation
    )
}

enum class MemberAction {
    DELETE_MEMBER, DELETE_LOCATION, BLOCK_USER, UNBLOCK_USER, REQUEST_LOCATION;
}