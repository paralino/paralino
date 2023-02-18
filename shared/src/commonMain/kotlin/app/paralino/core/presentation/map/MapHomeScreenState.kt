package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.utils.Emojis

sealed class MapHomeScreenState {
    abstract val currentUserImage: ImageModel

    data class Ready constructor(
        val groupId: String,
        val groupName: String,
        val groupImage: ImageModel,
        val userCount: Int,
        val inactiveUserCount: Int,
        override val currentUserImage: ImageModel,
        val members: List<GroupMemberItemState>,
        val markers: List<MapPinData>,
        val focusedMarker: FocusedMarkerData?,
        val sharingStatus: LocationSharingContract?
    ): MapHomeScreenState()
    data class Empty(
        override val currentUserImage: ImageModel = ImageModel.emoji(Emojis.robot),
    ): MapHomeScreenState()
}

sealed class MapHomeScreenEvent {
    data class Error(val error: Throwable) : MapHomeScreenEvent()
}