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
import kotlinx.datetime.Instant

sealed interface GroupMemberItemState {
    val memberState: GroupMemberState
    val keyId: String
    val lastPing: String?
    val actions: MemberActionsState
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
    override val actions: MemberActionsState,
    override val lastPing: String?,
) : GroupMemberItemState {
    override val keyId: String
        get() = location?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation: GroupMemberItemLocationState?
        get() = location
    override val singleValidLocation: GroupMemberItemLocationState?
        get() = location?.takeIf { it.visibility.isVisible() }
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    val locations: List<GroupMemberItemLocationState>,
    override val actions: MemberActionsState,
    val allowShowingDevices: Boolean,
    override val lastPing: String?
) : GroupMemberItemState {

    override val keyId: String
        get() = singleLocation?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation = locations.takeIf { it.size == 1 }?.first()
    override val singleValidLocation = singleLocation?.takeIf { it.visibility.isVisible() }

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
    val networkType: NetworkType?,
    val signalStrength: NetworkSignalStrength?,
    val canEdit: Boolean,
    val currentDevice: Boolean,
    val visibility: MemberLocationVisibilityType
) {
    constructor(
        memberLocation: MemberLocation,
        name: String? = memberLocation.name,
        image: ImageModel?,
        canEdit: Boolean,
        currentDevice: Boolean,
        showSpeedAndElevation: Boolean,
        visibility: MemberLocationVisibilityType,
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
        visibility = visibility
    )
}

data class MemberActionsState(
    val deleteMember: MemberActionItemState? = null,
    val deleteLocation: MemberActionItemState? = null,
    val blockUser: MemberActionItemState? = null,
    val unblockUser: MemberActionItemState? = null,
    val locationRequest: LocationRequestItemState? = null
) {
    val isActive = listOfNotNull(deleteMember, deleteLocation, blockUser, unblockUser, locationRequest).isNotEmpty()
}

data class MemberActionItemState(
    val type: MemberActionType,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
)

data class LocationRequestItemState(
    val isWaitingForUser: Boolean = false,
    val hasRequestError: Boolean = false,
    val hasDeviceSetupWrongError: Boolean = false,
    val hasDeviceUnreachableError: Boolean = false,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
    val lastLocationRequest: Instant? = null
) {
    val canInteract: Boolean = !isLoading && !isWaitingForUser
    val hasError = hasRequestError || hasDeviceSetupWrongError || hasDeviceUnreachableError
}

enum class MemberActionType {
    DELETE_MEMBER, DELETE_LOCATION, BLOCK_USER, UNBLOCK_USER;
}

enum class MemberLocationVisibilityType {
    VISIBLE_ALWAYS, VISIBLE_ONCE, HIDDEN, MISSING;

    fun isVisible(): Boolean {
        return this == VISIBLE_ALWAYS || this == VISIBLE_ONCE
    }

    fun show(): MemberLocationVisibilityType {
        return when (this) {
            HIDDEN -> VISIBLE_ONCE
            else -> this
        }
    }

    fun tryToHide(): MemberLocationVisibilityType {
        return when (this) {
            VISIBLE_ONCE -> HIDDEN
            else -> this
        }
    }
}