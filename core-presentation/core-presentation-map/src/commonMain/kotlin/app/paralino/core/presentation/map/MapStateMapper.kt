package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.models.base.GroupWithMembersOrUserSpace
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.user.UserMeData
import app.paralino.core.presentation.common.GroupMemberItemLocationState
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.GroupMemberState
import app.paralino.core.presentation.common.LocationHistoryActionItemState
import app.paralino.core.presentation.common.LocationRequestItemState
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.MemberActionItemState
import app.paralino.core.presentation.common.MemberActionType
import app.paralino.core.presentation.common.MemberActionsState
import app.paralino.core.presentation.common.MemberLocationVisibilityType
import app.paralino.core.presentation.common.MemberNavigateItemState
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MapStateMapper(
    private val appPrefs: AppPrefs,
    private val userGroupLocal: UserGroupLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val remoteConfig: RemoteConfigRepository,
    private val memberActionHandler: MemberActionHandler,
    private val locationRequestDuration: Duration,
    private val locationRequestDelay: Duration,
    private val premiumStatusManager: PremiumStatusManager
) {

    suspend fun buildReadyState(
        groupOrSpace: GroupWithMembersOrUserSpace,
        places: List<GroupPlace>,
        activeSharingContracts: List<LocationSharingContract>,
        userData: UserMeData,
        previousState: MapHomeScreenState.Ready?,
        isSatelliteModeEnabled: Boolean,
        alertDialog: MainAlertDialog?,
        isFirstTimeInPersonalSpace: Boolean,
    ): MapHomeScreenState.Ready {
        val groupName = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.name
            is GroupWithMembersOrUserSpace.PersonalSpace -> MR.strings.personalSpace_spaceTitle.localizedString()
        }

        val groupImage = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.image
            is GroupWithMembersOrUserSpace.PersonalSpace -> userData.userImage ?: ImageModel.emoji(
                Emojis.robot
            )
        }

        val userCount = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.memberCount
            is GroupWithMembersOrUserSpace.PersonalSpace -> groupOrSpace.space.devices.size
        }

        val planConfig = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.group.planConfig(remoteConfig.getConfigOnce())
            is GroupWithMembersOrUserSpace.PersonalSpace -> groupOrSpace.space.plan
        }

        val isOverLimits = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> {
                userGroupLocal.allGroupsState.value.data
                    ?.firstOrNull { it.id == groupOrSpace.id.id }
                    ?.isGroupOverLimits(remoteConfig.getConfigOnce())
                    ?: false
            }
            is GroupWithMembersOrUserSpace.PersonalSpace -> groupOrSpace.space.isSpaceOverLimits()
        }

        val isVerified = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.isGroupVerified
            is GroupWithMembersOrUserSpace.PersonalSpace -> true // Personal space is always verified
        }

        val selectedGroupId: String? = appPrefs.selectedGroupId

        val selectedGroupImage: ImageModel? = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.image
            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                userGroupLocal.allGroupsState.value.data?.firstOrNull { it.id == selectedGroupId }?.image
            }
        }

        val selectedGroupOwnerId: String? = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.ownerUserId
            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                userGroupLocal.allGroupsState.value.data?.firstOrNull { it.id == selectedGroupId }?.group?.ownerUserId
            }
        }

        val isCurrentUserOwnerOfSelectedGroup: Boolean = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> groupOrSpace.group.isCurrentUserOwner
            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                userGroupLocal.allGroupsState.value.data?.firstOrNull { it.id == selectedGroupId }?.group?.isCurrentUserOwner == true
            }
        }

        val mapInteractionsType = previousState?.mapInteractionType ?: appPrefs.mapInteractions

        val membersData = buildMembersData(
            groupOrSpace = groupOrSpace,
            groupPlanConfig = planConfig,
            previousState = previousState
        )

        val placesData = places
            .filter { it.data.data?.lat != null && it.data.data?.lng != null }
            .map { PlaceListItemData(it) }

        val markers = buildMarkers(
            groupOrSpace = groupOrSpace,
            membersData = membersData,
            placesData = placesData,
            previousFocusedMarker = previousState?.markers?.firstOrNull { it.focused }
        )

        val focusType = determineFocusType(
            previousFocusType = previousState?.mapHomeScreenFocusType,
            membersData = membersData,
            placesData = placesData,
            markers = markers
        )

        val mapSheetSelectedContentType = previousState?.mapSheetSelectedContentType
            ?: MapSheetSelectedContentType.MEMBERS

        // only keep previous tracking marker if it still exists
        val trackingMarker = previousState?.trackingMarker?.takeIf { oldMarker ->
            markers.any { it.markerId == oldMarker.markerId }
        }

        // Calculate active and idle counts
        val (activeCount, idleCount) = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> {
                val members = groupOrSpace.group.members
                Pair(
                    members.filter { it.isActive }.size,
                    members.filter { it.hasLocation && !it.isActive }.size
                )
            }

            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                val devices = groupOrSpace.space.devices
                Pair(
                    devices.count { it.location != null },
                    0 // Personal space doesn't have idle concept
                )
            }
        }

        val selectedGroupContract = activeSharingContracts.firstOrNull {
            it.groupOrSpaceId.isGroup && it.groupOrSpaceId == groupOrSpace.id
        }
        val personalSharingContract = activeSharingContracts.firstOrNull {
            it.groupOrSpaceId.isPersonalSpace
        }
        val activeSharingContract = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> selectedGroupContract
            is GroupWithMembersOrUserSpace.PersonalSpace -> personalSharingContract
        }
        val mapProvider = premiumStatusManager.defaultMapProvider()

        return MapHomeScreenState.Ready(
            groupOrSpaceId = groupOrSpace.id,
            groupOrSpaceName = groupName,
            groupOrSpaceImage = groupImage,
            selectedGroupId = selectedGroupId,
            selectedGroupOwnerId = selectedGroupOwnerId,
            isCurrentUserOwnerOfSelectedGroup = isCurrentUserOwnerOfSelectedGroup,
            selectedGroupImage = selectedGroupImage,
            isFirstTimeInPersonalSpace = isFirstTimeInPersonalSpace,
            userCount = userCount,
            activeUserCount = activeCount,
            idleUserCount = idleCount,
            currentUserImage = userData.userImage ?: ImageModel.emoji(Emojis.robot),
            allMembersOrDevices = membersData,
            places = placesData,
            markers = markers,
            trackingMarker = trackingMarker,
            mapHomeScreenFocusType = focusType,
            mapSheetSelectedContentType = mapSheetSelectedContentType,
            selectedGroupSharingContract = selectedGroupContract,
            personalSpaceSharingContract = personalSharingContract,
            sharingEndTime = activeSharingContract?.endTimeUTC?.let {
                valueFormatter.fromUTCtoMapSharingTime(
                    timeUTC = it,
                    splitTimeToOneLine = true
                )
            },
            verified = isVerified,
            isMapFocusRequested = previousState?.isMapFocusRequested ?: false,
            mapInteractionType = mapInteractionsType,
            desiredMapSheetPosition = previousState?.desiredMapSheetPosition,
            alertDialog = alertDialog,
            isSatelliteModeEnabled = isSatelliteModeEnabled,
            groupPlanConfig = planConfig,
            isOverLimit = isOverLimits,
            androidMapSDKProvider = mapProvider
        )
    }

    private fun buildMembersData(
        groupOrSpace: GroupWithMembersOrUserSpace,
        groupPlanConfig: PlanConfig,
        previousState: MapHomeScreenState.Ready?
    ): List<GroupMemberItemStateWithOneLocation> {

        return when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> {
                buildGroupMembersData(
                    groupMembers = groupOrSpace.group.members,
                    groupId = groupOrSpace.group.id,
                    groupPlanConfig = groupPlanConfig,
                    previousState = previousState
                )
            }

            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                buildPersonalSpaceDevicesData(
                    space = groupOrSpace.space,
                    groupPlanConfig = groupPlanConfig,
                    previousState = previousState
                )
            }
        }
    }

    private fun buildGroupMembersData(
        groupMembers: List<MemberModelWithLocations>,
        groupId: String,
        groupPlanConfig: PlanConfig,
        previousState: MapHomeScreenState.Ready?
    ): List<GroupMemberItemStateWithOneLocation> {
        return groupMembers.flatMap { member ->
            val memberState = GroupMemberState(
                member = member.member,
                isCurrentUser = member.member.userId == appPrefs.userId,
                canHaveExpandButton = false
            ).copy(paralinoId = null)

            if (member.locations.isEmpty()) {
                listOf(
                    GroupMemberItemStateWithOneLocation(
                        memberState = memberState,
                        actions = MemberActionsState(
                            history = LocationHistoryActionItemState(
                                isLoading = false
                            )
                        ),
                        singleLocation = null,
                        deviceId = null,
                        isHighlighted = memberState.isCurrentUser,
                        isCollapsedOrMinimized = false
                    )
                )
            } else {
                member.locations.map { location ->
                    val previousStateForLocation = previousState?.allMembersOrDevices?.firstOrNull {
                        it.memberState.memberId == memberState.memberId && it.singleLocation?.locationId == location.id
                    }

                    val locationVisibility = determineLocationVisibility(
                        location = location,
                        previousState = previousStateForLocation
                    )

                    val actions = buildMemberActions(
                        memberName = member.member.name,
                        memberUserId = member.member.userId,
                        location = location,
                        locationVisibility = locationVisibility,
                        groupId = groupId,
                        groupPlanConfig = groupPlanConfig,
                        previousState = previousStateForLocation
                    )

                    GroupMemberItemStateWithOneLocation(
                        memberState = memberState,
                        actions = actions,
                        deviceId = location.deviceId,
                        singleLocation = GroupMemberItemLocationState(
                            memberLocation = location,
                            name = location.name,
                            image = member.member.image,
                            canEdit = false,
                            currentDevice = location.deviceId == appPrefs.deviceId,
                            showSpeedAndElevation = true,
                            visibility = locationVisibility,
                            formatter = valueFormatter
                        ),
                        isHighlighted = memberState.isCurrentUser,
                        isCollapsedOrMinimized = false
                    )
                }
            }
        }
            .sortedByDescending { it.singleLocation?.deviceStatus == MemberDeviceStatusType.ACTIVE }
            .sortedByDescending { it.singleLocation?.deviceStatus == MemberDeviceStatusType.ACTIVE_NO_LOCATION }
            .sortedByDescending { it.singleLocation?.deviceStatus?.isIdlingOrActive }
            .sortedByDescending { it.singleLocation?.visibility?.isVisible() }
    }

    private fun buildPersonalSpaceDevicesData(
        space: UserPersonalSpaceModel,
        groupPlanConfig: PlanConfig,
        previousState: MapHomeScreenState.Ready?
    ): List<GroupMemberItemStateWithOneLocation> {

        return space.devices.map { device ->
            val memberState = GroupMemberState(
                groupId = UserGroup.groupPersonalDevicesId,
                memberId = device.deviceId, // Use device ID as member ID to make it unique
                userId = device.ownerUserId,
                name = device.deviceNameOrNull ?: let {
                    if (device.deviceId == appPrefs.deviceId) {
                        MR.strings.personalSpace_current_device.localizedString()
                    } else {
                        device.deviceType?.toString() ?: "?"
                    }
                },
                image = ImageModel.emoji(Emojis.mobilePhone),
                paralinoId = null,
                isCurrentUser = device.ownerUserId == appPrefs.userId,
                isVerified = true,
                canHaveExpandButton = false
            )

            val location = device.location
            val previousStateForLocation = previousState?.allMembersOrDevices?.firstOrNull {
                it.memberState.memberId == memberState.memberId && it.singleLocation?.locationId == location?.id
            }

            val locationVisibility = location?.let {
                determineLocationVisibility(
                    location = it,
                    previousState = previousStateForLocation
                )
            } ?: MemberLocationVisibilityType.MISSING

            val actions = buildMemberActions(
                memberName = space.user.name,
                memberUserId = space.user.id,
                location = location,
                locationVisibility = locationVisibility,
                groupId = UserGroup.groupPersonalDevicesId,
                groupPlanConfig = groupPlanConfig,
                previousState = previousStateForLocation
            )

            GroupMemberItemStateWithOneLocation(
                memberState = memberState,
                actions = actions,
                deviceId = device.deviceId,
                singleLocation = location?.let {
                    GroupMemberItemLocationState(
                        memberLocation = it,
                        name = null,
                        image = ImageModel.emoji(Emojis.mobilePhone),
                        canEdit = false,
                        currentDevice = it.deviceId == appPrefs.deviceId,
                        showSpeedAndElevation = true,
                        visibility = locationVisibility,
                        formatter = valueFormatter
                    )
                },
                isHighlighted = device.deviceId == appPrefs.deviceId,
                isCollapsedOrMinimized = false
            )
        }
            .sortedByDescending { it.singleLocation?.deviceStatus == MemberDeviceStatusType.ACTIVE }
            .sortedByDescending { it.singleLocation?.deviceStatus == MemberDeviceStatusType.ACTIVE_NO_LOCATION }
            .sortedByDescending { it.singleLocation?.deviceStatus?.isIdlingOrActive }
            .sortedByDescending { it.singleLocation?.visibility?.isVisible() }
    }

    private fun determineLocationVisibility(
        location: MemberLocation,
        previousState: GroupMemberItemStateWithOneLocation?
    ): MemberLocationVisibilityType {
        return if (location.isLocationAvailableButOld) {
            previousState?.singleLocation?.visibility
                ?.takeIf { it == MemberLocationVisibilityType.VISIBLE_ONCE }
                ?: MemberLocationVisibilityType.HIDDEN
        } else if (location.hasLocation) {
            MemberLocationVisibilityType.VISIBLE_ALWAYS
        } else {
            MemberLocationVisibilityType.MISSING
        }
    }

    private fun buildMemberActions(
        memberName: String,
        memberUserId: String,
        location: MemberLocation?,
        locationVisibility: MemberLocationVisibilityType,
        groupId: String,
        groupPlanConfig: PlanConfig,
        previousState: GroupMemberItemStateWithOneLocation?
    ): MemberActionsState {

        val navigate = location?.locationData?.data?.takeIf {
            locationVisibility.isVisible()
        }?.let {
            MemberNavigateItemState(
                lat = it.lat,
                lng = it.lng,
                title = memberName,
                isEnabled = true
            )
        }

        val deleteLocation = takeIf {
            location != null &&
            memberActionHandler.canDeleteLocation(
                groupId = groupId.asGroupOrSpaceId(),
                isCurrentUser = memberUserId == appPrefs.userId,
                isCurrentDevice = location.deviceId == appPrefs.deviceId,
                hasLocation = locationVisibility.isVisible()
            )
        }?.let {
            MemberActionItemState(
                type = MemberActionType.DELETE_LOCATION,
                isLoading = previousState?.actions?.deleteLocation?.isLoading ?: false,
            )
        }

        val history = LocationHistoryActionItemState(
            isLoading = false
        )

        val locationRequest = location?.let { location ->
            buildLocationRequestAction(
                location = location,
                groupPlanConfig = groupPlanConfig,
                previousState = previousState
            )
        }

        return MemberActionsState(
            navigate = navigate,
            deleteLocation = deleteLocation,
            history = history,
            locationRequest = locationRequest
        )
    }

    private fun buildLocationRequestAction(
        location: MemberLocation,
        groupPlanConfig: PlanConfig,
        previousState: GroupMemberItemStateWithOneLocation?
    ): LocationRequestItemState? {

        if (!location.canRequestLocation) return null

        val isRequestInProgress = previousState?.actions?.locationRequest?.run {
            isLoading && !isWaitingForUser
        } ?: false

        val hasUserResponded =
            (previousState?.actions?.locationRequest?.lastLocationRequest?.toEpochMilliseconds()
                ?: 0) <
                    (location.status.data?.timeUTC?.fromUTCtoInstant()?.plus(2.seconds)
                        ?.toEpochMilliseconds() ?: 0)

        val isRequestExpired = previousState?.actions?.locationRequest?.lastLocationRequest
            ?.plus(locationRequestDuration)
            ?.let { it < nowUTC() }
            ?: true

        val canRequestLocationAgain = previousState?.actions?.locationRequest?.lastLocationRequest
            ?.plus(locationRequestDelay)
            ?.let { it < nowUTC() }
            ?: true

        return if (hasUserResponded && !isRequestInProgress) {
            LocationRequestItemState(
                isEnabled = canRequestLocationAgain,
                lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest,
                lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant(),
                isPremiumFeatureEnabled = groupPlanConfig.planTypeEnum.isPremium
            )
        } else if (isRequestExpired && !isRequestInProgress) {
            LocationRequestItemState(
                isEnabled = canRequestLocationAgain,
                lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest,
                lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant(),
                isPremiumFeatureEnabled = groupPlanConfig.planTypeEnum.isPremium
            )
        } else {
            previousState.actions.locationRequest?.copy(
                lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant()
            )
        }
    }

    private fun buildMarkers(
        groupOrSpace: GroupWithMembersOrUserSpace,
        membersData: List<GroupMemberItemStateWithOneLocation>,
        placesData: List<PlaceListItemData>,
        previousFocusedMarker: MapPinData?
    ): List<MapPinData> {

        val memberMarkers = when (groupOrSpace) {
            is GroupWithMembersOrUserSpace.Group -> {
                groupOrSpace.group.members.flatMap { member ->
                    member.locations
                        .filter { it.locationData.data != null }
                        .map { location ->
                            val memberLocationState = membersData.first {
                                it.singleLocation?.locationId == location.id
                            }
                            val oldCurrentMarker = previousFocusedMarker?.takeIf {
                                it.markerId == memberLocationState.keyId
                            }

                            val marker = MapPinData(
                                location = location,
                                name = listOfNotNull(
                                    member.member.name,
                                    location.nameOrNull
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { "($it)" }
                                ).joinToString(" "),
                                image = member.member.image,
                                timeInfo = valueFormatter.getLastLocationPing(location)
                            )

                            marker.copy(
                                markerId = memberLocationState.keyId,
                                focused = oldCurrentMarker != null,
                                hidden = oldCurrentMarker?.hidden ?: marker.hidden
                            )
                        }
                }
            }

            is GroupWithMembersOrUserSpace.PersonalSpace -> {
                groupOrSpace.space.devices.mapNotNull { device ->
                    device.location
                        ?.takeIf { it.locationData.data != null }
                        ?.let { location ->
                            val memberLocationState = membersData.firstOrNull {
                                it.memberState.memberId == device.deviceId && it.singleLocation?.locationId == location.id
                            } ?: return@mapNotNull null

                            val oldCurrentMarker = previousFocusedMarker?.takeIf {
                                it.markerId == memberLocationState.keyId
                            }

                            val deviceName = location.deviceData.data?.deviceName
                                ?: "Device ${device.deviceType ?: "--"}"
                            val marker = MapPinData(
                                location = location,
                                name = location.nameOrNull?.takeIf { it.isNotBlank() }
                                    ?: deviceName,
                                image = ImageModel.emoji(Emojis.mobilePhone),
                                timeInfo = valueFormatter.getLastLocationPing(location)
                            )

                            marker.copy(
                                markerId = memberLocationState.keyId,
                                focused = oldCurrentMarker != null,
                                hidden = oldCurrentMarker?.hidden ?: marker.hidden
                            )
                        }
                }
            }
        }

        val placeMarkers = placesData.map { place ->
            place.makeMapPin(groupOrSpaceId = groupOrSpace.id)
                .copy(
                    active = true,
                    focused = previousFocusedMarker?.markerId == place.id
                )
        }

        return (placeMarkers + memberMarkers)
            .sortedBy { it.focused }
    }

    private fun determineFocusType(
        previousFocusType: MapHomeScreenFocusType?,
        membersData: List<GroupMemberItemStateWithOneLocation>,
        placesData: List<PlaceListItemData>,
        markers: List<MapPinData>
    ): MapHomeScreenFocusType {

        // only keep previous focus type if it still exists
        val validPreviousFocusType = previousFocusType?.takeIf { oldFocusType ->
            when (oldFocusType) {
                is MapHomeScreenFocusType.MEMBER -> {
                    membersData.any { it.keyId == oldFocusType.memberKeyId }
                }

                is MapHomeScreenFocusType.PLACE -> {
                    placesData.any { it.id == oldFocusType.placeId }
                }

                else -> true
            }
        }

        return validPreviousFocusType
            ?: MapHomeScreenFocusType.ALL_MEMBERS.takeIf { markers.any { it.pinType == MapPinType.MEMBER && !it.hidden } }
            ?: MapHomeScreenFocusType.ALL_PLACES.takeIf { markers.any { it.pinType == MapPinType.PLACE && !it.hidden } }
            ?: MapHomeScreenFocusType.NONE
    }
} 