package com.zbapp.locationtrack.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.map.MapHomeViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository


class MapViewModel constructor(
    app: Application,
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
) : AndroidViewModel(app) {

    private val coreVM = MapHomeViewModel(locationRepo, sharingManager, groupRepo, viewModelScope)

    val uiState = coreVM.uiState
    val locationRefreshSubscription = coreVM.locationRefreshSubscription

    fun switchGroup(groupId: String) = coreVM.switchGroup(groupId)
    suspend fun getAllGroups() = coreVM.getAllGroups()
    fun clearMyLocation() = coreVM.clearMyLocation()

}