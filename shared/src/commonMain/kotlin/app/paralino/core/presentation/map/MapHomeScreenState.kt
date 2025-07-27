package app.paralino.core.presentation.map

import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.presentation.settings.generic.IOSNavigationAppType
import app.paralino.core.provider.current
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel
    abstract val isSatelliteMode: Boolean
    abstract val alertDialog: MainAlertDialog?

    val deviceType: DeviceType = DeviceType.current

    data class Ready constructor(
        val groupOrSpaceId: GroupOrSpaceId,
        val groupName: String,
        val groupImage: ImageModel,
        val userCount: Int,
        val activeUserCount: Int,
        val idleUserCount: Int,
        override val currentUserImage: ImageModel,

        val members: List<GroupMemberItemStateWithOneLocation>,
        val places: List<PlaceListItemData>,
        val markers: List<MapPinData>,
        val trackingMarker: FocusedMarkerData?,
        val mapHomeScreenFocusType: MapHomeScreenFocusType,
        val mapSheetSelectedContentType: MapSheetSelectedContentType,

        val sharingStatus: LocationSharingContract?,
        val sharingEndTime: String?,
        val verified: Boolean,
        val groupPlanConfig: PlanConfig,
        override val alertDialog: MainAlertDialog?,
        override val isSatelliteMode: Boolean = false
    ) : MapHomeScreenState() {
        val visibleMarkers: List<MapPinData> = markers.filter { !it.hidden }
        val allMembersVerified: Boolean get() = members.all { it.memberState.isVerified }
        val inactiveUserCount: Int get() = userCount - activeUserCount - idleUserCount

        val focusedTypeId: String = when (mapHomeScreenFocusType) {
            is MapHomeScreenFocusType.PLACE -> mapHomeScreenFocusType.placeId
            is MapHomeScreenFocusType.MEMBER -> mapHomeScreenFocusType.memberKeyId
            else -> mapHomeScreenFocusType.toString()
        }
        val focusedPlace: PlaceListItemData? =
            (mapHomeScreenFocusType as? MapHomeScreenFocusType.PLACE)?.let { item ->
                places.firstOrNull { it.id == item.placeId }
            }

        val focusedMember: GroupMemberItemState? =
            (mapHomeScreenFocusType as? MapHomeScreenFocusType.MEMBER)?.let { item ->
                members.firstOrNull { it.keyId == item.memberKeyId }
            }
    }

    data class Empty(
        override val currentUserImage: ImageModel = ImageModel.emoji(Emojis.robot),
        override val isSatelliteMode: Boolean = false,
        override val alertDialog: MainAlertDialog? = null,
    ) : MapHomeScreenState()
}

fun MapHomeScreenState.asReady(): MapHomeScreenState.Ready? = this as? MapHomeScreenState.Ready?

sealed class MapHomeScreenEvent {
    data object AddGroup : MapHomeScreenEvent()
    data object ShowFreePlanInfo : MapHomeScreenEvent()
    data object OpenPaywall : MapHomeScreenEvent()
    data object OpenSettings : MapHomeScreenEvent()
    data object OpenGroupPicker : MapHomeScreenEvent()
    data object OpenPersonalSpace : MapHomeScreenEvent()
    data class ShowPlanLimitAlert(val limit: Int?) : MapHomeScreenEvent()
    data class OpenSharingStart(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaces(val groupId: String) : MapHomeScreenEvent()
    data class OpenCreatePlace(val groupId: String) : MapHomeScreenEvent()
    data class OpenGroupDetails(val groupId: String) : MapHomeScreenEvent()
    data class OpenPlaceDetails(val groupId: String, val placeId: String) : MapHomeScreenEvent()
    data class OpenLocationHistory(val groupId: String, val memberUserId: String) : MapHomeScreenEvent()
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
    data class OpenGroupDetails(val groupId: GroupOrSpaceId) : MapHomeScreenUiEvent
    data class SwitchGroup(val groupId: GroupOrSpaceId?) : MapHomeScreenUiEvent
    data object TryCreateGroup : MapHomeScreenUiEvent
    data object OnFocusOnMembersClick : MapHomeScreenUiEvent
    data object OnFocusOnPlacesClick : MapHomeScreenUiEvent
    data object OnOpenPlacesScreen : MapHomeScreenUiEvent
    data object OnOpenCreatePlaceScreen : MapHomeScreenUiEvent
    data object OnMapTouch : MapHomeScreenUiEvent
    data object OnGroupPickerClick : MapHomeScreenUiEvent
    data object OnGroupDetailsClick : MapHomeScreenUiEvent
    data object OnSharingStartClick : MapHomeScreenUiEvent
    data object OnSettingsClick : MapHomeScreenUiEvent
    data object OnMapModeSwitch : MapHomeScreenUiEvent
    data object OnCloseFocusedItemClick : MapHomeScreenUiEvent
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

    data object ALL_PLACES : MapHomeScreenFocusType
    data object ALL_MEMBERS : MapHomeScreenFocusType

    val areMembersFocused: Boolean get() = this == ALL_MEMBERS
    val arePlacesFocused: Boolean get() = this == ALL_PLACES
    val isMemberOrPlaceFocused: Boolean get() = this is MEMBER || this is PLACE

    fun toggle(target: MapHomeScreenFocusType): MapHomeScreenFocusType {
        return when (this) {
            NONE -> target
            else -> if (target != this) target else NONE
        }
    }
}

data class MapScreenLifecycleData(
    val groupId: GroupOrSpaceId,
    val isAppFocused: Boolean,
    val isUiFocused: Boolean,
    val mapFocusType: MapHomeScreenFocusType,
)

sealed interface GroupWithMembersOrUserSpace {
    val id: GroupOrSpaceId
    data class Group(val group: UserGroupWithMembers): GroupWithMembersOrUserSpace {
        override val id: GroupOrSpaceId = group.id.asGroupOrSpaceId()
    }
    data class PersonalSpace(val space: UserPersonalSpaceModel): GroupWithMembersOrUserSpace {
        override val id: GroupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
    }
}