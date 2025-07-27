package app.paralino.core.presentation.map

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.models.user.UserMeData
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.LocationRequestItemState
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.presentation.groups.MemberNavigateItemState
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.seconds

class MapStateMapper(
    private val appPrefs: AppPrefs,
    private val valueFormatter: IValueFormatter,
    private val remoteConfig: RemoteConfigRepository,
    private val memberActionHandler: MemberActionHandler,
    private val locationRequestDuration: kotlin.time.Duration,
    private val locationRequestDelay: kotlin.time.Duration
) {

    suspend fun buildReadyState(
        groupData: UserGroupWithMembers,
        places: List<GroupPlace>,
        sharingStatus: LocationSharingContract?,
        userData: UserMeData,
        previousState: MapHomeScreenState.Ready?,
        isSatelliteMode: Boolean,
        alertDialog: MainAlertDialog?
    ): MapHomeScreenState.Ready {

        val groupPlanConfig = groupData.group.planConfig(remoteConfig.getConfigOnce())

        val membersData = buildMembersData(
            groupMembers = groupData.members,
            groupId = groupData.id,
            groupPlanConfig = groupPlanConfig,
            previousState = previousState
        )

        val placesData = places
            .filter { it.data.data?.lat != null && it.data.data?.lng != null }
            .map { PlaceListItemData(it) }

        val markers = buildMarkers(
            groupId = groupData.id,
            members = groupData.members,
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

        return MapHomeScreenState.Ready(
            groupId = groupData.id,
            groupName = groupData.name,
            groupImage = groupData.image,
            userCount = groupData.memberCount,
            activeUserCount = groupData.members.filter { it.isActive }.size,
            idleUserCount = groupData.members.filter { it.hasLocation && !it.isActive }.size,
            currentUserImage = userData.userImage ?: ImageModel.emoji(Emojis.robot),
            members = membersData,
            places = placesData,
            markers = markers,
            trackingMarker = trackingMarker,
            mapHomeScreenFocusType = focusType,
            mapSheetSelectedContentType = mapSheetSelectedContentType,
            sharingStatus = sharingStatus,
            sharingEndTime = sharingStatus?.endTimeUTC?.let {
                valueFormatter.fromUTCtoMapSharingTime(
                    timeUTC = it,
                    splitTimeToOneLine = true
                )
            },
            verified = groupData.isGroupVerified,
            alertDialog = alertDialog,
            isSatelliteMode = isSatelliteMode,
            groupPlanConfig = groupPlanConfig
        )
    }

    private fun buildMembersData(
        groupMembers: List<MemberModelWithLocations>,
        groupId: String,
        groupPlanConfig: PlanConfig,
        previousState: MapHomeScreenState.Ready?
    ): List<GroupMemberItemStateWithOneLocation> {

        return groupMembers.flatMap { member ->
            if (member.locations.isEmpty()) {
                listOf(
                    GroupMemberItemStateWithOneLocation(
                        memberState = GroupMemberState(
                            member = member.member,
                            isCurrentUser = member.member.userId == appPrefs.userId,
                            canHaveExpandButton = false
                        ),
                        actions = MemberActionsState(),
                        location = null
                    )
                )
            } else {
                val memberState = GroupMemberState(
                    member = member.member,
                    isCurrentUser = member.member.userId == appPrefs.userId,
                    canHaveExpandButton = false
                )

                member.locations.map { location ->
                    val previousStateForLocation = previousState?.members?.firstOrNull {
                        it.memberState.memberId == memberState.memberId && it.location?.locationId == location.id
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
                        location = GroupMemberItemLocationState(
                            memberLocation = location,
                            name = location.name,
                            image = member.member.image,
                            canEdit = false,
                            currentDevice = location.deviceId == appPrefs.deviceId,
                            showSpeedAndElevation = true,
                            visibility = locationVisibility,
                            formatter = valueFormatter
                        )
                    )
                }
            }
        }
            .sortedByDescending { it.location?.deviceStatus == MemberDeviceStatusType.ACTIVE }
            .sortedByDescending { it.location?.deviceStatus == MemberDeviceStatusType.ACTIVE_NO_LOCATION }
            .sortedByDescending { it.location?.deviceStatus?.isIdlingOrActive }
            .sortedByDescending { it.location?.visibility?.isVisible() }
    }

    private fun determineLocationVisibility(
        location: MemberLocation,
        previousState: GroupMemberItemStateWithOneLocation?
    ): MemberLocationVisibilityType {
        return if (location.isLocationAvailableButOld) {
            previousState?.location?.visibility
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
        location: MemberLocation,
        locationVisibility: MemberLocationVisibilityType,
        groupId: String,
        groupPlanConfig: PlanConfig,
        previousState: GroupMemberItemStateWithOneLocation?
    ): MemberActionsState {

        val navigate = location.locationData.data?.takeIf {
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

        val history = MemberActionItemState(
            type = MemberActionType.LOCATION_HISTORY,
            isLoading = false,
        )

        val locationRequest = buildLocationRequestAction(
            location = location,
            groupPlanConfig = groupPlanConfig,
            previousState = previousState
        )

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
            previousState.actions.locationRequest.copy(
                lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant()
            )
        }
    }

    private fun buildMarkers(
        groupId: String,
        members: List<MemberModelWithLocations>,
        membersData: List<GroupMemberItemStateWithOneLocation>,
        placesData: List<PlaceListItemData>,
        previousFocusedMarker: MapPinData?
    ): List<MapPinData> {

        val memberMarkers = members.flatMap { member ->
            member.locations
                .filter { it.locationData.data != null }
                .map { location ->
                    val memberLocationState = membersData.first {
                        it.location?.locationId == location.id
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

        val placeMarkers = placesData.map { place ->
            MapPinData(
                groupId = groupId,
                state = place
            ).copy(
                active = true,
                focused = previousFocusedMarker?.markerId == place.id
            )
        }

        return placeMarkers + memberMarkers
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