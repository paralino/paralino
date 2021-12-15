package com.zbapp.locationtrack.ui.groups.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.core.models.Payload
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.mapToPayload
import kotlinx.coroutines.flow.map

class GroupDetailViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository
): ViewModel() {

    val groupDetails = groupRepo.getGroup(groupId)
            .map {
                GroupDetailsUiModel(
                    id = groupId,
                    name = it.name,
                    image = it.emoji,
                    memberCount = it.members.size
                )
            }
            .mapToPayload { it }
            .asLiveData()

    fun deleteGroup(): LiveData<Payload<Unit>> {
        return groupRepo.deleteGroup(groupId)
            .mapToPayload { it }
            .asLiveData()
    }

}

data class GroupDetailsUiModel(
    val id: String,
    val name: String,
    val image: String,
    val memberCount: Int
)
