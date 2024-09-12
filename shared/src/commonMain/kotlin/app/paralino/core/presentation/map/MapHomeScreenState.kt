package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel
    abstract val isSatelliteMode: Boolean
    abstract val alertDialog: MainAlertDialog?

    data class Ready constructor(
        val groupId: String,
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
    data object GroupsPlanLimitReached : MapHomeScreenEvent()
}

sealed interface MapHomeScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : MapHomeScreenUiEvent
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