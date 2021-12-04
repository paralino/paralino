package com.zbapp.locationtrack.ui.groups.detail.members

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.mapToPayload
import kotlinx.coroutines.flow.map

class GroupMembersViewModel constructor(
    private val groupId: String,
    private val groupRepo: GroupRepository
) : ViewModel() {

    val groupMembers = groupRepo.getGroupLocally(groupId)
        .map { group ->
            group.members.map {
                GroupMemberItemData(
                    member = it,
                    canDelete = true
//                    canDelete = group.ownerId == config.userId && it.userId != config.userId
                )
            }
        }
        .asLiveData()

    fun removeMember(member: MemberModel): LiveData<Payload<Unit>> {
        return groupRepo.removeMember(member)
            .mapToPayload { Unit }
            .asLiveData()
    }

}