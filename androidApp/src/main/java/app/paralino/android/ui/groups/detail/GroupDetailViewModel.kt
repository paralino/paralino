package app.paralino.android.ui.groups.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ILocationStatusProvider

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationSharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider
) : ViewModel() {

    private val coreVm = GroupDetailViewModel(
        groupId,
        groupRepo,
        placesRepo,
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