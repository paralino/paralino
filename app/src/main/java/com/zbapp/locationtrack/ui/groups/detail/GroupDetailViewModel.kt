package com.zbapp.locationtrack.ui.groups.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.ui.groups.SharingStartFragmentArgs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager
): ViewModel() {

    private val sharingStatus = locationSharingManager.isSharingActiveForGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val groupDetails = combine(groupRepo.getGroupWithMembers(groupId), sharingStatus)
            { group, sharingActive ->
                GroupDetailsUiModel(
                    id = groupId,
                    name = group.name,
                    image = group.emoji,
                    memberCount = group.members.size,
                    sharingActive = sharingActive
                )
            }
            .mapToPayload { it }
            .asLiveData()

    fun deleteGroup(): LiveData<Payload<Unit>> {
        return groupRepo.deleteGroup(groupId)
            .mapToPayload { it }
            .asLiveData()
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
    val image: String,
    val memberCount: Int,
    val sharingActive: Boolean
)
