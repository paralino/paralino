package app.paralino.android.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.dashboard.DashboardViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.ILocationStatusProvider

class DashboardViewModel(
    app: Application,
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    val locationStatusProvider: ILocationStatusProvider,
) : AndroidViewModel(app) {

    private val coreVM =
        DashboardViewModel(locationSharing, groupRepository, locationStatusProvider, viewModelScope)

    val uiState = coreVM.uiState
    val events = coreVM.events

    fun loadDashboard() = coreVM.loadDashboard()

    fun stopTracking(groupId: String) = coreVM.stopTracking(groupId)

}