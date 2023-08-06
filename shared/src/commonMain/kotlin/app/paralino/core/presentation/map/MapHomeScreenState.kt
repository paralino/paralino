package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState
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
        val markers: List<MapPinData>,
        val focusedMarker: FocusedMarkerData?,
        val sharingStatus: LocationSharingContract?,
        override val isSatelliteMode: Boolean = false
    ) : MapHomeScreenState() {
        val allMembersVerified: Boolean get() = members.all { it.memberState.isVerified }
        val inactiveUserCount: Int get() = userCount - activeUserCount - idleUserCount
    }

    data class Empty(
        override val currentUserImage: ImageModel = ImageModel.emoji(Emojis.robot),
        override val isSatelliteMode: Boolean = false
    ) : MapHomeScreenState()
}

sealed class MapHomeScreenEvent {
    data class Error(val error: Throwable) : MapHomeScreenEvent()
}