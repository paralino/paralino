package app.paralino.core.presentation.map

import androidx.compose.ui.unit.Dp
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.models.settings.IOSNavigationAppType
import app.paralino.core.models.settings.MapInteractionsType
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.provider.current
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.GroupMemberItemEvent
import app.paralino.core.presentation.common.GroupMemberItemState
import app.paralino.core.presentation.common.GroupMemberItemStateWithFullStateLocations
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.MemberActionsState
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.utils.ConditionErrorType

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel
    abstract val isSatelliteModeEnabled: Boolean
    abstract val alertDialog: MainAlertDialog?
    abstract val selectedGroupId: String?
    abstract val selectedGroupImage: ImageModel?
    abstract val isPersonalSpace: Boolean
    abstract val isCurrentUserOwnerOfSelectedGroup: Boolean
    abstract val selectedGroupOwnerId: String?
    abstract val isSheetEnabled: Boolean
    abstract val defaultSheetPosition: BottomSheetPosition
    abstract val androidMapSDKProvider: MapsSDKProvider?
    abstract val canCollapseMapHeader: Boolean

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
        override val selectedGroupOwnerId: String?,
        override val isCurrentUserOwnerOfSelectedGroup: Boolean,
        override val selectedGroupImage: ImageModel?,
        override val currentUserImage: ImageModel,

        val isFirstTimeInPersonalSpace: Boolean,

        val userCount: Int,
        val activeUserCount: Int,
        val idleUserCount: Int,

        val allMembersOrDevices: List<GroupMemberItemStateWithOneLocation>,
        val places: List<PlaceListItemData>,
        val privacyZones: List<PlaceListItemData>,
        val markers: List<MapPinData>,
        val trackingMarker: FocusedMarkerData?,
        val mapHomeScreenFocusType: MapHomeScreenFocusType,
        val mapSheetSelectedContentType: MapSheetSelectedContentType,

        val personalSpaceSharingContract: LocationSharingContract?,
        val selectedGroupSharingContract: LocationSharingContract?,

        val isMapFocusRequested: Boolean,
        val mapInteractionType: MapInteractionsType,
        val desiredMapSheetPosition: BottomSheetPosition?,

        val sharingEndTime: String?,
        val verified: Boolean,
        val groupPlanConfig: PlanConfig,
        val isOverLimit: Boolean,
        val errors: List<ConditionErrorType>,
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
                val default = desiredMapSheetPosition ?: BottomSheetPosition.HALF_EXPANDED
                when (mapInteractionType) {
                    MapInteractionsType.DEVICE_DEFAULT -> default
                    MapInteractionsType.NONE -> default
                    MapInteractionsType.MINIMIZE -> {
                        if (isMapFocusRequested) {
                            BottomSheetPosition.COLLAPSED
                        } else {
                            default
                        }
                    }
                    MapInteractionsType.COLLAPSED -> {
                        if (isMapFocusRequested) {
                            BottomSheetPosition.COLLAPSED_MINIMISED
                        } else {
                            default
                        }
                    }
                }
            }
        }

        val canCollapseFocusedMemberItem = when (mapInteractionType) {
            MapInteractionsType.MINIMIZE,
            MapInteractionsType.COLLAPSED -> isMapFocusRequested
            else -> false
        }

        override val canCollapseMapHeader = when (defaultSheetPosition) {
            BottomSheetPosition.EXPANDED -> false
            else -> when (mapInteractionType) {
                MapInteractionsType.MINIMIZE -> {
                    if (mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused) {
                        canCollapseFocusedMemberItem
                    } else {
                        false
                    }
                }
                MapInteractionsType.COLLAPSED -> isMapFocusRequested || mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused
                else -> false
            }
        }

        val focusedPlace: PlaceListItemData? = mapHomeScreenFocusType.asPlace?.let { item ->
            places.firstOrNull { it.id == item.placeId }
        }

        val focusedPrivacyZone: PlaceListItemData? = mapHomeScreenFocusType.asPrivacyZone?.let { item ->
            privacyZones.firstOrNull { it.id == item.privacyZoneId }
        }

        val focusedMember: GroupMemberItemStateWithOneLocation? = mapHomeScreenFocusType.asMember
            ?.let { item ->
                allMembersOrDevices.firstOrNull { it.keyId == item.memberKeyId }
            }?.copy(
                isCollapsedOrMinimized = canCollapseFocusedMemberItem
            )
    }

    data class Empty(
        val groupsEmptyState: GroupsInitEmptyScreenState?,
        override val selectedGroupId: String?,
        override val selectedGroupOwnerId: String?,
        override val isCurrentUserOwnerOfSelectedGroup: Boolean,
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
        override val canCollapseMapHeader = false
    }
}

fun MapHomeScreenState.asReady(): MapHomeScreenState.Ready? = this as? MapHomeScreenState.Ready?

sealed class MapHomeScreenEvent {
    data object AskNotificationPermission: MapHomeScreenEvent()
    data object AskLocationPermission: MapHomeScreenEvent()
    data object AskBackgroundLocationPermission: MapHomeScreenEvent()
    data object AskLocationServices: MapHomeScreenEvent()
    data object OpenPersonalCodes: MapHomeScreenEvent()
    data object OpenGroupCreateScreen : MapHomeScreenEvent()
    data class OpenPaywall(val type: BillingScreenSourceType) : MapHomeScreenEvent()
    data object OpenSettings : MapHomeScreenEvent()
    data object OpenMapProviderSettings : MapHomeScreenEvent()
    data class OpenGroupPicker(val showPersonalSpace: Boolean) : MapHomeScreenEvent()
    data object OpenPersonalSpace : MapHomeScreenEvent()
    data class SharePersonalCode(val content: String): MapHomeScreenEvent()
    data class ShowPlanLimitAlert(val limit: Int?) : MapHomeScreenEvent()
    data class OpenSharingStart(val groupId: String) : MapHomeScreenEvent()
    data class OpenMemberInviteScreen(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaces(val groupId: String) : MapHomeScreenEvent()
    data class OpenCreatePlace(val groupId: String, val location: LatLng?) : MapHomeScreenEvent()
    data class OpenGroupDetails(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaceDetails(val groupId: String, val placeId: String) : MapHomeScreenEvent()
    data class OpenPrivacyZones(val groupId: String) : MapHomeScreenEvent()
    data class OpenCreatePrivacyZone(val groupId: String, val location: LatLng?) : MapHomeScreenEvent()
    data class OpenPrivacyZoneDetails(val groupId: String, val zoneId: String) : MapHomeScreenEvent()
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
    data class OnPrivacyZoneClick(val zone: PlaceListItemData) : MapHomeScreenUiEvent
    data class OnMemberItemEvent(val event: GroupMemberItemEvent) : MapHomeScreenUiEvent
    data class OnGroupsEmptyStateEvent(val event: GroupsInitEmptyScreenUiEvent) : MapHomeScreenUiEvent
    data class OpenGroupDetails(val groupId: GroupOrSpaceId) : MapHomeScreenUiEvent
    data class SwitchViewSource(val viewSource: MapScreenViewSourceType) : MapHomeScreenUiEvent
    data class OnMapSheetPositionChange(val position: BottomSheetPosition) : MapHomeScreenUiEvent
    data class OnScreenDensityChange(val maxWidthDp: Dp, val maxHeightDp: Dp) : MapHomeScreenUiEvent
    data object OpenPersonalLocationHistory : MapHomeScreenUiEvent
    data object SwitchToPersonalSpace : MapHomeScreenUiEvent
    data object SwitchToGroups : MapHomeScreenUiEvent
    data object TryCreateGroup : MapHomeScreenUiEvent
    data object OnFocusOnMembersClick : MapHomeScreenUiEvent
    data object OnFocusOnPlacesClick : MapHomeScreenUiEvent
    data object OnFocusOnPrivacyZonesClick : MapHomeScreenUiEvent
    data object OnOpenPlacesScreen : MapHomeScreenUiEvent
    data object OnOpenPrivacyZonesScreen : MapHomeScreenUiEvent
    data object OnOpenMemberInviteScreen : MapHomeScreenUiEvent
    data class OnOpenCreatePlaceScreen(val location: LatLng?) : MapHomeScreenUiEvent
    data class OnOpenCreatePrivacyZoneScreen(val location: LatLng?) : MapHomeScreenUiEvent
    data object OnMapTouch : MapHomeScreenUiEvent
    data class OnMapClick(val location: LatLng) : MapHomeScreenUiEvent
    data object OnRefocusSelectedDeviceItem : MapHomeScreenUiEvent
    data object OnGroupPickerClick : MapHomeScreenUiEvent
    data object OnGroupDetailsClick : MapHomeScreenUiEvent
    data object OnSharingStartClick : MapHomeScreenUiEvent
    data object OnSettingsClick : MapHomeScreenUiEvent
    data object OnMapModeClick : MapHomeScreenUiEvent
    data object OnMapModeLongClick : MapHomeScreenUiEvent
    data object OnCloseFocusedItemClick : MapHomeScreenUiEvent
    data object OnBackClicked : MapHomeScreenUiEvent
    data object ConfirmPersonalSpaceIntro : MapHomeScreenUiEvent
    data object CloseLocationHistory : MapHomeScreenUiEvent
    data object OnOverLimitsBannerTap : MapHomeScreenUiEvent
    data class OnConditionErrorItemClick(val item: ConditionErrorType) : MapHomeScreenUiEvent
    data class OpenPaywall(val type: BillingScreenSourceType) : MapHomeScreenUiEvent
}

enum class MapSheetSelectedContentType {
    PLACES, MEMBERS, PRIVACY_ZONES
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