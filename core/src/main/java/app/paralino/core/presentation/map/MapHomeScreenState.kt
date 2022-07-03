package app.paralino.core.presentation.map

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.GroupMemberItemState

data class MapHomeScreenState constructor(
    val groupId: String,
    val groupName: String,
    val groupImage: ImageModel,
    val userCount: Int,
    val inactiveUserCount: Int,
    val members: List<GroupMemberItemState>,
    val markers: List<MapMarkerState>,
    val sharingStatus: LocationSharingContract?
)

data class MapMarkerState constructor(
    val groupId: String,
    val focused: Boolean,
    val active: Boolean,
    val image: ImageModel?,
    val memberData: GroupMemberItemState,
    val locationData: LocationDataModel
)

sealed class MapHomeScreenEvent {
    data class Error(val error: Throwable): MapHomeScreenEvent()
}