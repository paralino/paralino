package app.paralino.core.presentation.map

import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithFullStateLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.settings.generic.IOSNavigationAppType
import app.paralino.core.presentation.settings.generic.MapsSDKProvider
import app.paralino.core.provider.current
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.map.BottomSheetPosition

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel
    abstract val isSatelliteModeEnabled: Boolean
    abstract val alertDialog: MainAlertDialog?
    abstract val selectedGroupId: String?
    abstract val selectedGroupImage: ImageModel?
    abstract val isPersonalSpace: Boolean
    abstract val isSheetEnabled: Boolean
    abstract val defaultSheetPosition: BottomSheetPosition
    abstract val androidMapSDKProvider: MapsSDKProvider?

    val deviceType: DeviceType = DeviceType.current
    val hasSatelliteMode: Boolean get() = when (deviceType) {
        DeviceType.Android -> androidMapSDKProvider?.hasSatelliteMaps != false
        DeviceType.iOS -> true
    }
    val isSatelliteModeActive: Boolean get() = if (hasSatelliteMode) isSatelliteModeEnabled else false

    data class Ready constructor(
        val groupOrSpaceId: GroupOrSpaceId,
        val groupOrSpaceName: String,
        val groupOrSpaceImage: ImageModel,

        override val selectedGroupId: String?,
        override val selectedGroupImage: ImageModel?,
        override val currentUserImage: ImageModel,

        val isFirstTimeInPersonalSpace: Boolean,

        val userCount: Int,
        val activeUserCount: Int,
        val idleUserCount: Int,

        val allMembersOrDevices: List<GroupMemberItemStateWithOneLocation>,
        val places: List<PlaceListItemData>,
        val markers: List<MapPinData>,
        val trackingMarker: FocusedMarkerData?,
        val mapHomeScreenFocusType: MapHomeScreenFocusType,
        val mapSheetSelectedContentType: MapSheetSelectedContentType,

        val personalSpaceSharingContract: LocationSharingContract?,
        val selectedGroupSharingContract: LocationSharingContract?,

        val sharingEndTime: String?,
        val verified: Boolean,
        val groupPlanConfig: PlanConfig,
        val isOverLimit: Boolean,
        override val androidMapSDKProvider: MapsSDKProvider,
        override val alertDialog: MainAlertDialog?,
        override val isSatelliteModeEnabled: Boolean = false
    ) : MapHomeScreenState() {
        override val isPersonalSpace = groupOrSpaceId.isPersonalSpace
        val hasPersonalSpaceIntroBanner = isPersonalSpace && isFirstTimeInPersonalSpace

        val selectedSharingContract = if (isPersonalSpace) {
            personalSpaceSharingContract
        } else {
            selectedGroupSharingContract
        }

        val membersWithDevices: List<GroupMemberItemState> = allMembersOrDevices
            .groupBy { it.memberState }
            .map {
                if (it.value.size > 1) {
                    GroupMemberItemStateWithFullStateLocations(
                        memberState = it.key,
                        items = it.value,
                        actions = MemberActionsState(),
                        allowShowingDevices = true,
                        lastLocationPing = null,
                        lastStatusPing = null
                    )
                } else {
                    it.value.first()
                }
            }

        val visibleMarkers: List<MapPinData> = markers.filter { !it.hidden }
        val allMembersVerified: Boolean get() = allMembersOrDevices.all { it.memberState.isVerified }
        val inactiveUserCount: Int get() = userCount - activeUserCount - idleUserCount

        val focusedPlace: PlaceListItemData? =
            (mapHomeScreenFocusType as? MapHomeScreenFocusType.PLACE)?.let { item ->
                places.firstOrNull { it.id == item.placeId }
            }

        val focusedMember: GroupMemberItemState? =
            (mapHomeScreenFocusType as? MapHomeScreenFocusType.MEMBER)?.let { item ->
                allMembersOrDevices.firstOrNull { it.keyId == item.memberKeyId }
            }

        val locationHistoryMode: MapHomeScreenFocusType.LOCATION_HISTORY? = mapHomeScreenFocusType.asLocationHistory

        override val isSheetEnabled: Boolean = if (isPersonalSpace) {
            !isFirstTimeInPersonalSpace && !mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused
        } else {
            !mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused
        }

        override val defaultSheetPosition = if (mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused) {
            BottomSheetPosition.HIDDEN
        } else {
            if (isPersonalSpace && isFirstTimeInPersonalSpace) {
                BottomSheetPosition.EXPANDED
            } else {
                BottomSheetPosition.HALF_EXPANDED
            }
        }
    }

    data class Empty(
        val groupsEmptyState: GroupsInitEmptyScreenState?,
        override val selectedGroupId: String?,
        override val selectedGroupImage: ImageModel?,
        override val currentUserImage: ImageModel,
        override val isPersonalSpace: Boolean,
        override val androidMapSDKProvider: MapsSDKProvider?,
        override val isSatelliteModeEnabled: Boolean = false,
        override val alertDialog: MainAlertDialog? = null,
    ) : MapHomeScreenState() {
        override val isSheetEnabled: Boolean = false
        override val defaultSheetPosition: BottomSheetPosition = if (groupsEmptyState != null) {
            BottomSheetPosition.EXPANDED
        } else {
            BottomSheetPosition.HIDDEN
        }
    }
}

fun MapHomeScreenState.asReady(): MapHomeScreenState.Ready? = this as? MapHomeScreenState.Ready?

sealed class MapHomeScreenEvent {
    data object AskNotificationPermission: MapHomeScreenEvent()
    data object OpenPersonalCodes: MapHomeScreenEvent()
    data object OpenGroupCreateScreen : MapHomeScreenEvent()
    data object OpenPaywall : MapHomeScreenEvent()
    data object OpenSettings : MapHomeScreenEvent()
    data object OpenMapProviderSettings : MapHomeScreenEvent()
    data class OpenGroupPicker(val showPersonalSpace: Boolean) : MapHomeScreenEvent()
    data object OpenPersonalSpace : MapHomeScreenEvent()
    data class SharePersonalCode(val content: String): MapHomeScreenEvent()
    data class ShowPlanLimitAlert(val limit: Int?) : MapHomeScreenEvent()
    data class OpenSharingStart(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaces(val groupId: String) : MapHomeScreenEvent()
    data class OpenCreatePlace(val groupId: String, val location: LatLng?) : MapHomeScreenEvent()
    data class OpenGroupDetails(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaceDetails(val groupId: String, val placeId: String) : MapHomeScreenEvent()
    data class OpenExternalNavigation(
        val lat: Double,
        val lng: Double,
        val name: String?,
        val navigationApp: IOSNavigationAppType
    ) : MapHomeScreenEvent()
}

sealed interface MapHomeScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : MapHomeScreenUiEvent
    data class UiLifecycleChange(val active: Boolean) : MapHomeScreenUiEvent
    data class OnMarkerClick(val pin: MapPinData) : MapHomeScreenUiEvent
    data class OnPlaceClick(val place: PlaceListItemData) : MapHomeScreenUiEvent
    data class OnMemberItemEvent(val event: GroupMemberItemEvent) : MapHomeScreenUiEvent
    data class OnGroupsEmptyStateEvent(val event: GroupsInitEmptyScreenUiEvent) : MapHomeScreenUiEvent
    data class OpenGroupDetails(val groupId: GroupOrSpaceId) : MapHomeScreenUiEvent
    data class SwitchViewSource(val viewSource: MapScreenViewSourceType) : MapHomeScreenUiEvent
    data object OpenPersonalLocationHistory : MapHomeScreenUiEvent
    data object SwitchToPersonalSpace : MapHomeScreenUiEvent
    data object SwitchToGroups : MapHomeScreenUiEvent
    data object TryCreateGroup : MapHomeScreenUiEvent
    data object OnFocusOnMembersClick : MapHomeScreenUiEvent
    data object OnFocusOnPlacesClick : MapHomeScreenUiEvent
    data object OnOpenPlacesScreen : MapHomeScreenUiEvent
    data class OnOpenCreatePlaceScreen(val location: LatLng?) : MapHomeScreenUiEvent
    data object OnMapTouch : MapHomeScreenUiEvent
    data object OnGroupPickerClick : MapHomeScreenUiEvent
    data object OnGroupDetailsClick : MapHomeScreenUiEvent
    data object OnSharingStartClick : MapHomeScreenUiEvent
    data object OnSettingsClick : MapHomeScreenUiEvent
    data object OnMapModeSwitch : MapHomeScreenUiEvent
    data object OnCloseFocusedItemClick : MapHomeScreenUiEvent
    data object OnBackClicked : MapHomeScreenUiEvent
    data object ConfirmPersonalSpaceIntro : MapHomeScreenUiEvent
    data object CloseLocationHistory : MapHomeScreenUiEvent
    data object OnOverLimitsBannerTap : MapHomeScreenUiEvent
}

enum class MapSheetSelectedContentType {
    PLACES, MEMBERS
}

sealed interface MapHomeScreenFocusType {
    data object NONE : MapHomeScreenFocusType
    data class PLACE(val placeId: String) : MapHomeScreenFocusType
    data class MEMBER(
        val memberId: String,
        val memberUserId: String,
        val memberLocationId: String?,
        val memberKeyId: String,
        val isIdleActive: Boolean,
        val forceZoom: Boolean
    ) : MapHomeScreenFocusType

    data class LOCATION_HISTORY(
        val member: MapHomeScreenFocusType.MEMBER?,
        val groupId: String,
        val memberUserId: String,
        val deviceId: String?
    ) : MapHomeScreenFocusType

    data object ALL_PLACES : MapHomeScreenFocusType
    data object ALL_MEMBERS : MapHomeScreenFocusType

    val areMembersFocused: Boolean get() = this == ALL_MEMBERS
    val arePlacesFocused: Boolean get() = this == ALL_PLACES
    val asLocationHistory: LOCATION_HISTORY? get() = this as? LOCATION_HISTORY
    val asMember: MEMBER? get() = this as? MEMBER
    val isLocationHistoryFocused: Boolean get() = this is LOCATION_HISTORY
    val isMemberOrPlaceFocused: Boolean get() = this is MEMBER || this is PLACE
    val isMemberPlaceOrHistoryFocused: Boolean get() = isMemberOrPlaceFocused || isLocationHistoryFocused
}

sealed interface MapScreenViewSourceType {
    val id: GroupOrSpaceId?

    data class DefaultGroupOrSpace(override val id: GroupOrSpaceId?) : MapScreenViewSourceType
    data class DefaultGroup(override val id: GroupOrSpaceId?) : MapScreenViewSourceType

    data object PersonalSpace : MapScreenViewSourceType {
        override val id: GroupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
    }
}

data class MapScreenLifecycleData(
    val groupId: GroupOrSpaceId,
    val planConfig: PlanConfig,
    val isAppFocused: Boolean,
    val isUiFocused: Boolean,
    val mapFocusType: MapHomeScreenFocusType,
)