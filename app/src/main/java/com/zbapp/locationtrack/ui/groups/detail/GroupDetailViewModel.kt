package com.zbapp.locationtrack.ui.groups.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.groups.GroupDetailViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.ILocationStatusProvider

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider
) : ViewModel() {

    private val coreVm = GroupDetailViewModel(
        groupId,
        groupRepo,
        locationSharingManager,
        locationStatusProvider,
        viewModelScope
    )

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun leaveOrDeleteGroup() = coreVm.leaveOrDeleteGroup()
    fun removeMember(memberUserId: String) = coreVm.removeMember(memberUserId)
    fun stopSharing() = coreVm.stopSharing()

}