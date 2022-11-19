package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState

data class MapHomeScreenState constructor(
    val groupId: String,
    val groupName: String,
    val groupImage: ImageModel,
    val userCount: Int,
    val inactiveUserCount: Int,
    val members: List<GroupMemberItemState>,
    val markers: List<MapPinData>,
    val focusedMarkerId: String?,
    val sharingStatus: LocationSharingContract?
)

sealed class MapHomeScreenEvent {
    data class Error(val error: Throwable) : MapHomeScreenEvent()
}