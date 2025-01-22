package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.dataIfReady
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
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
    val lastLocationPing: String?
    val lastStatusPing: String?
    val actions: MemberActionsState
    val singleLocation: GroupMemberItemLocationState?
    val singleVisibleLocation: GroupMemberItemLocationState?
}

data class GroupMemberState constructor(
    val groupId: String,
    val memberId: String,
    val userId: String,
    val name: String,
    val image: ImageModel?,
    val paralinoId: String?,
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
        paralinoId = member.paralinoId.dataIfReady?.formattedCode,
        isCurrentUser = isCurrentUser,
        isVerified = member.isVerified,
        canHaveExpandButton = canHaveExpandButton
    )
}

data class GroupMemberItemStateWithOneLocation constructor(
    override val memberState: GroupMemberState,
    val location: GroupMemberItemLocationState?,
    override val actions: MemberActionsState,
) : GroupMemberItemState {
    override val keyId: String
        get() = location?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation: GroupMemberItemLocationState?
        get() = location
    override val singleVisibleLocation: GroupMemberItemLocationState?
        get() = location?.takeIf { it.visibility.isVisible() }
    override val lastLocationPing: String? get() = singleLocation?.lastLocationPing
    override val lastStatusPing: String? get() = singleVisibleLocation?.lastStatusPing
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    val locations: List<GroupMemberItemLocationState>,
    override val actions: MemberActionsState,
    val allowShowingDevices: Boolean,
    override val lastLocationPing: String?,
    override val lastStatusPing: String?
) : GroupMemberItemState {

    override val keyId: String
        get() = singleLocation?.locationId?.takeIf { it.isNotBlank() }?.plus(memberState.memberId)
            ?: memberState.memberId

    override val singleLocation = locations.takeIf { it.size == 1 }?.first()
    override val singleVisibleLocation = singleLocation?.takeIf { it.visibility.isVisible() }

    val filteredLastPing: String? = lastLocationPing?.takeIf { singleLocation != null }

    val statusTag: MemberDeviceStatusType?
        get() = singleLocation?.deviceStatus
            ?: locations.firstOrNull { it.deviceStatus == MemberDeviceStatusType.ACTIVE }?.deviceStatus
}

data class GroupMemberItemLocationState constructor(
    val locationId: String,
    val name: String?,
    val image: ImageModel,
    val actualBatteryLevel: Int?,
    val adjustedBatteryLevel: Int?,
    val isBatteryCharging: Boolean,
    val speed: String?,
    val elevation: String?,
    val lastLocationPing: String?,
    val lastStatusPing: String?,
    val accuracyMeters: Float?,
    val deviceStatus: MemberDeviceStatusType?,
    val onlineStatus: MemberDeviceOnlineStatusType?,
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
        actualBatteryLevel = memberLocation.status.data?.validBatteryLevel(checkForTime = false),
        adjustedBatteryLevel = memberLocation.status.data?.validBatteryLevel(checkForTime = true),
        isBatteryCharging = memberLocation.status.data?.isCharging ?: false,
        speed = memberLocation.locationData.data?.speed?.takeIf { showSpeedAndElevation }?.let { formatter.getSpeedSmall(it) },
        elevation = memberLocation.locationData.data?.altitude?.takeIf { showSpeedAndElevation }?.let { formatter.getDistanceSmall(it.toDouble()) },
        lastLocationPing = formatter.getLastLocationPing(memberLocation),
        lastStatusPing = formatter.getLastStatusPing(memberLocation),
        accuracyMeters = memberLocation.locationData.data?.accuracy,
        deviceStatus = memberLocation.locationAwareStatusType,
        onlineStatus = memberLocation.status.data?.onlineStatus,
        networkType = memberLocation.status.data?.networkType?.takeIf { showSpeedAndElevation },
        signalStrength = memberLocation.status.data?.signalStrength?.takeIf { showSpeedAndElevation },
        canEdit = canEdit,
        currentDevice = currentDevice,
        visibility = visibility
    )

    val visibleBatteryLevel: Int? = when(visibility) {
        MemberLocationVisibilityType.VISIBLE_ALWAYS -> adjustedBatteryLevel
        MemberLocationVisibilityType.VISIBLE_ONCE -> actualBatteryLevel
        MemberLocationVisibilityType.HIDDEN -> adjustedBatteryLevel
        MemberLocationVisibilityType.MISSING -> adjustedBatteryLevel
    }
}

data class MemberActionsState(
    val deleteMember: MemberActionItemState? = null,
    val deleteLocation: MemberActionItemState? = null,
    val blockUser: MemberActionItemState? = null,
    val unblockUser: MemberActionItemState? = null,
    val locationRequest: LocationRequestItemState? = null,
    val securityAction: Boolean = false
) {
    val isActive = listOfNotNull(deleteMember, deleteLocation, blockUser, unblockUser, locationRequest, securityAction.takeIf { it }).isNotEmpty()
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
    val isPremiumFeatureEnabled: Boolean,
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