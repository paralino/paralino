package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.utils.Emojis

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel
    abstract val isSatelliteMode: Boolean

    data class Ready constructor(
        val groupId: String,
        val groupName: String,
        val groupImage: ImageModel,
        val userCount: Int,
        val activeUserCount: Int,
        val idleUserCount: Int,
        override val currentUserImage: ImageModel,

        val members: List<GroupMemberItemState>,
        val places: List<PlaceListItemData>,
        val markers: List<MapPinData>,
        val trackingMarker: FocusedMarkerData?,
        val mapHomeScreenFocusType: MapHomeScreenFocusType,

        val sharingStatus: LocationSharingContract?,
        override val isSatelliteMode: Boolean = false
    ) : MapHomeScreenState() {
        val allMembersVerified: Boolean get() = members.all { it.memberState.isVerified }
        val inactiveUserCount: Int get() = userCount - activeUserCount - idleUserCount
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
        override val isSatelliteMode: Boolean = false
    ) : MapHomeScreenState()
}

sealed class MapHomeScreenEvent {
    data class Error(val error: Throwable) : MapHomeScreenEvent()
}

sealed interface MapHomeScreenFocusType {
    data object NONE : MapHomeScreenFocusType
    data class PLACE(val placeId: String) : MapHomeScreenFocusType
    data class MEMBER(val memberKeyId: String) : MapHomeScreenFocusType
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