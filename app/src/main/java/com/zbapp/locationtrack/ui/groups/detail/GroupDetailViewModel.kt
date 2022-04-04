package com.zbapp.locationtrack.ui.groups.detail

import androidx.lifecycle.*
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.user.ImageModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.exceptions.NotFoundException
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager
) : ViewModel() {

    private val sharingStatus = locationSharingManager.sharingStatusForGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val groupDetails = combine(groupRepo.getGroupWithMembers(groupId), sharingStatus)
    { group, sharingActive ->
        GroupDetailsUiModel(
            id = groupId,
            name = group.name,
            image = group.image,
            memberCount = group.memberCount,
            sharingActive = sharingActive,
            isMyGroup = group.isCurrentUserOwner
        )
    }
        .mapToPayload { it }
        .stateIn(viewModelScope, SharingStarted.Lazily, Payload.Loading)

    val groupDetailsLiveData = groupDetails.asLiveData().distinctUntilChanged()

    fun leaveOrDeleteGroup(): LiveData<Payload<Unit>> {
        return groupDetails.value.data?.let {
            if (it.isMyGroup) {
                groupRepo.deleteGroup(groupId)
            } else {
                groupRepo.leaveGroup(groupId)
            }.mapToPayload { it }.asLiveData()
        } ?: liveData { emit(Payload.Error(NotFoundException("Group not loaded"))) }
    }

    fun isSharingActive(): Boolean {
        return sharingStatus.value
    }

    fun stopSharing() {
        viewModelScope.launch {
            locationSharingManager.stopSharing(groupId)
        }
    }

}

data class GroupDetailsUiModel(
    val id: String,
    val name: String,
    val image: ImageModel,
    val memberCount: Int,
    val sharingActive: Boolean,
    val isMyGroup: Boolean
)
