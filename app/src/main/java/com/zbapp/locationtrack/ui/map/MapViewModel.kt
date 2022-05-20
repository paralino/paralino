package com.zbapp.locationtrack.ui.map

import android.app.Application
import androidx.lifecycle.*
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupWithMembers
import com.zbapp.core.presentation.dashboard.DashboardViewModel
import com.zbapp.core.presentation.groups.MemberAction
import com.zbapp.core.presentation.map.MapHomeViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMemberItemData
import com.zbapp.locationtrack.utils.extensions.fromUTCtoMapSharingTime
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MapViewModel constructor(
    app: Application,
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository
) : AndroidViewModel(app) {

    private val coreVM = MapHomeViewModel(locationRepo, sharingManager, groupRepo, viewModelScope)

    val uiState = coreVM.uiState
    val locationRefreshSubscription = coreVM.locationRefreshSubscription

    fun switchGroup(groupId: String) = coreVM.switchGroup(groupId)
    suspend fun getAllGroups() = coreVM.getAllGroups()
    fun clearMyLocation() = coreVM.clearMyLocation()

}

data class MapUiModel(
    val group: UserGroupWithMembers,
    val memberListData: List<GroupMemberItemData>
)