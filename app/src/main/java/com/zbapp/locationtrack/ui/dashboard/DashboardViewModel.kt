package com.zbapp.locationtrack.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.dashboard.DashboardViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.utils.ILocationStatusProvider

class DashboardViewModel(
    app: Application,
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val loginManager: LoginManager,
    val locationStatusProvider: ILocationStatusProvider,
) : AndroidViewModel(app) {

    private val coreVM =
        DashboardViewModel(locationSharing, groupRepository, locationStatusProvider, viewModelScope)

    val uiState = coreVM.uiState
    val events = coreVM.events

    fun loadDashboard() = coreVM.loadDashboard()

    fun stopTracking(groupId: String) = coreVM.stopTracking(groupId)

}