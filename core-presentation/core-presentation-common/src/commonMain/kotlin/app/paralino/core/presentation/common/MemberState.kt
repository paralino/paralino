package app.paralino.core.presentation.common

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.dataIfReady
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isOlderThan
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

sealed class GroupMemberItemState() {
    abstract val memberState: GroupMemberState
    abstract val keyId: String
    abstract val lastLocationPing: String?
    abstract val lastStatusPing: String?
    abstract val actions: MemberActionsState
    abstract val singleLocation: GroupMemberItemLocationState?
    abstract val allowShowingDevices: Boolean
    abstract val compactMultiDeviceMode: Boolean
    abstract val isHighlighted: Boolean
    abstract val isCollapsedOrMinimized: Boolean
    abstract val locations: List<GroupMemberItemLocationState>

    abstract val singleVisibleLocation: GroupMemberItemLocationState?
    abstract val filteredLastPing: String?
    abstract val statusTag: MemberDeviceStatusType?

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(markerId = this.keyId, type = MapPinType.MEMBER)
    }
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
    val deviceId: String?,
    override val singleLocation: GroupMemberItemLocationState?,
    override val actions: MemberActionsState,
    override val isHighlighted: Boolean,
    override val isCollapsedOrMinimized: Boolean,
) : GroupMemberItemState() {
    override val keyId: String = singleLocation?.keyId
        ?.takeIf { it.isNotBlank() }
//        ?.plus(memberState.memberId)
        ?: deviceId
        ?: memberState.memberId

    override val lastLocationPing: String? = singleLocation?.lastLocationPing
    override val locations = listOfNotNull(singleLocation)

    override val singleVisibleLocation: GroupMemberItemLocationState? = singleLocation?.takeIf { it.visibility.isVisible() }
    override val filteredLastPing: String? = lastLocationPing?.takeIf { singleLocation != null }
    override val statusTag: MemberDeviceStatusType? = singleLocation?.deviceStatus
        ?: locations.firstOrNull { it.deviceStatus == MemberDeviceStatusType.ACTIVE }?.deviceStatus

    override val lastStatusPing: String? = singleVisibleLocation?.lastStatusPing
    override val allowShowingDevices: Boolean = false
    override val compactMultiDeviceMode: Boolean = false
}

data class GroupMemberItemStateWithFullStateLocations constructor(
    override val memberState: GroupMemberState,
    val items: List<GroupMemberItemStateWithOneLocation>,
    override val actions: MemberActionsState,
    override val allowShowingDevices: Boolean,
    override val lastLocationPing: String?,
    override val lastStatusPing: String?
) : GroupMemberItemState() {

    override val isCollapsedOrMinimized: Boolean = false
    override val isHighlighted: Boolean = memberState.isCurrentUser
    override val locations = items.mapNotNull { it.singleLocation }
    override val singleLocation = locations.takeIf { it.size == 1 }?.first()
    override val keyId: String = singleLocation?.keyId
        ?.takeIf { it.isNotBlank() }
//        ?.plus(memberState.memberId)
        ?: memberState.memberId

    override val singleVisibleLocation: GroupMemberItemLocationState? = singleLocation?.takeIf { it.visibility.isVisible() }
    override val filteredLastPing: String? = lastLocationPing?.takeIf { singleLocation != null }
    override val statusTag: MemberDeviceStatusType? = singleLocation?.deviceStatus
        ?: locations.firstOrNull { it.deviceStatus == MemberDeviceStatusType.ACTIVE }?.deviceStatus
    override val compactMultiDeviceMode: Boolean = true
}

data class GroupMemberItemStateWithLocations constructor(
    override val memberState: GroupMemberState,
    override val locations: List<GroupMemberItemLocationState>,
    override val actions: MemberActionsState,
    override val allowShowingDevices: Boolean,
    override val lastLocationPing: String?,
    override val lastStatusPing: String?
) : GroupMemberItemState() {

    override val isCollapsedOrMinimized: Boolean = false
    override val isHighlighted: Boolean = memberState.isCurrentUser
    override val singleLocation = locations.takeIf { it.size == 1 }?.first()
    override val keyId: String = singleLocation?.keyId
        ?.takeIf { it.isNotBlank() }
//        ?.plus(memberState.memberId)
        ?: memberState.memberId

    override val singleVisibleLocation: GroupMemberItemLocationState? = singleLocation?.takeIf { it.visibility.isVisible() }
    override val filteredLastPing: String? = lastLocationPing?.takeIf { singleLocation != null }
    override val statusTag: MemberDeviceStatusType? = singleLocation?.deviceStatus
        ?: locations.firstOrNull { it.deviceStatus == MemberDeviceStatusType.ACTIVE }?.deviceStatus
    override val compactMultiDeviceMode: Boolean = false
}

data class GroupMemberItemLocationState constructor(
    val locationId: String,
    val deviceId: String,
    val name: String?,
    val image: ImageModel,
    val lat: Double?,
    val lng: Double?,
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
    val visibility: MemberLocationVisibilityType,
    val distanceFromMeText: String?,
    val atPlace: String?,
) {
    val keyId = locationId
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
        deviceId = memberLocation.deviceId,
        name = name,
        image = image ?: ImageModel.emoji(Emojis.lock),
        lat = memberLocation.locationData.data?.lat,
        lng = memberLocation.locationData.data?.lng,
        actualBatteryLevel = memberLocation.status.data?.validBatteryLevel(checkForTime = false),
        adjustedBatteryLevel = memberLocation.status.data?.validBatteryLevel(checkForTime = true),
        isBatteryCharging = memberLocation.status.data?.isCharging ?: false,
        speed = memberLocation.locationData.data?.speed?.takeIf { showSpeedAndElevation }?.let { formatter.getSpeedSmall(it) },
        elevation = memberLocation.locationData.data?.altitude
            ?.takeIf { showSpeedAndElevation }
            ?.let {
                formatter.getDistanceSmall(
                    meters = it.toDouble(),
                    distanceType = DistanceType.ELEVATION
                )
            },
        lastLocationPing = formatter.getLastLocationPing(memberLocation),
        lastStatusPing = formatter.getLastStatusPing(memberLocation),
        accuracyMeters = memberLocation.locationData.data?.accuracy,
        deviceStatus = memberLocation.locationAwareStatusType,
        onlineStatus = memberLocation.status.data?.onlineStatus,
        networkType = memberLocation.status.data?.networkType,
        signalStrength = memberLocation.status.data?.signalStrength,
        canEdit = canEdit,
        currentDevice = currentDevice,
        visibility = visibility,
        distanceFromMeText = null,
        atPlace = null,
    )

    val visibleBatteryLevel: Int? = when(visibility) {
        MemberLocationVisibilityType.VISIBLE_ALWAYS -> adjustedBatteryLevel
        MemberLocationVisibilityType.VISIBLE_ONCE -> actualBatteryLevel
        MemberLocationVisibilityType.HIDDEN -> adjustedBatteryLevel
        MemberLocationVisibilityType.MISSING -> adjustedBatteryLevel
    }

    val distancePlaceText: String? = listOfNotNull(
        atPlace,
        distanceFromMeText
    ).takeIf { it.isNotEmpty() }?.joinToString(separator = ", ")

    fun asFocusedMarker(): FocusedMarkerData {
        return FocusedMarkerData(markerId = this.keyId, type = MapPinType.MEMBER)
    }
}

data class MemberActionsState(
    val deleteMember: MemberActionItemState? = null,
    val deleteLocation: MemberActionItemState? = null,
    val blockUser: MemberActionItemState? = null,
    val unblockUser: MemberActionItemState? = null,
    val history: LocationHistoryActionItemState? = null,
    val navigate: MemberNavigateItemState? = null,
    val locationRequest: LocationRequestItemState? = null,
    val securityAction: Boolean = false
) {
    val isActive = listOfNotNull(
        deleteMember,
        deleteLocation,
        blockUser,
        unblockUser,
        locationRequest,
        history,
        navigate,
        securityAction.takeIf { it }
    ).isNotEmpty()
}

data class LocationHistoryActionItemState(
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
)

data class LocationRequestItemState(
    val isWaitingForUser: Boolean = false,
    val hasRequestError: Boolean = false,
    val hasDeviceSetupWrongError: Boolean = false,
    val hasDeviceUnreachableError: Boolean = false,
    val hasDeviceMaybeUnreachableError: Boolean = false,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
    val isPremiumFeatureEnabled: Boolean,
    val lastLocationRequest: Instant? = null,
    val lastDeviceUpdate: Instant? = null
) {
    val canInteract: Boolean = !isLoading && !isWaitingForUser
    val hasError = hasRequestError || hasDeviceSetupWrongError || hasDeviceUnreachableError || hasDeviceMaybeUnreachableError
    val isStatusRecent: Boolean = lastDeviceUpdate?.isOlderThan(10.minutes) == false
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