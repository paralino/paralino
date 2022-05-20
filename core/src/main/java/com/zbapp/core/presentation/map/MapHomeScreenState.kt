package com.zbapp.core.presentation.map

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.groups.GroupMemberItemState

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