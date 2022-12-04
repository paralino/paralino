package app.paralino.android.ui.groups.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ILocationStatusProvider

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val placesRepo: PlacesRepository,
    private val locationSharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider,
    private val groupRemove: GroupRemoveUseCase,
    private val groupMemberRemove: GroupMemberRemoveUseCase,
    private val groupRefresh: GroupRefreshUseCase,
) : ViewModel() {

    private val coreVm = GroupDetailViewModel(
        groupId,
        placesRepo,
        locationSharingManager,
        locationStatusProvider,
        groupRemove,
        groupMemberRemove,
        groupRefresh,
        viewModelScope
    )

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun leaveOrDeleteGroup() = coreVm.leaveOrDeleteGroup()
    fun removeMember(memberUserId: String) = coreVm.removeMember(memberUserId)
    fun removeMemberLocation(memberUserId: String, locationId: String) = coreVm.removeMemberLocation(memberUserId, locationId)
    fun stopSharing() = coreVm.stopSharing()

}