package com.zbapp.locationtrack.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.presentation.share.SharingStartViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.ILocationStatusProvider


class SharingViewModel constructor(
    app: Application,
    private val groupId: String?,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager,
    private val locationStatusProvider: ILocationStatusProvider,
) : AndroidViewModel(app) {

    private val coreVm =
        SharingStartViewModel(
            groupId,
            groupRepo,
            locationSharingManager,
            locationStatusProvider,
            viewModelScope
        )

    val uiState = coreVm.uiState
    val events = coreVm.events

    suspend fun getAllGroups() = coreVm.getAllGroups()
    fun setSelectedGroup(groupId: String) = coreVm.setSelectedGroup(groupId)

    fun startSharing() = coreVm.startSharing()
    fun proceedWithSharing() = coreVm.proceedWithSharing()
    fun sharingDurationChanged(index: Int) = coreVm.sharingDurationChanged(index)
    fun approximateLocationChanged(index: Int) = coreVm.approximateLocationChanged(index)
    fun turnOnApproximateLocationSharing(state: Boolean) =
        coreVm.turnOnApproximateLocationSharing(state)

    fun turnOnBatterySharing(state: Boolean) = coreVm.turnOnBatterySharing(state)
    fun turnOnSpeedSharing(state: Boolean) = coreVm.turnOnSpeedSharing(state)
    fun turnOnElevationSharing(state: Boolean) = coreVm.turnOnElevationSharing(state)

}