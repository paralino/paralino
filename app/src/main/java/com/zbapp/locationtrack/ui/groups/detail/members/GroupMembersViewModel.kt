package com.zbapp.locationtrack.ui.groups.detail.members

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.shared.list.EmptyStateData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import kotlinx.coroutines.flow.map

class GroupMembersViewModel constructor(
    app: Application,
    private val groupId: String,
    private val groupRepo: GroupRepository
) : AndroidViewModel(app) {

    val groupMembers: LiveData<GroupMemberUiModel> = groupRepo.getGroupWithMembersLocally(groupId)
        .map { group ->
            val members = group.members
                .sortedBy { it.name }
                .sortedBy { it.userId == config.userId }
                .map { member ->
                    GroupMemberItemData(
                        member = member,
                        actionItem = GroupMemberItemData.MemberAction.DELETE_MEMBER.takeIf {
                            group.isCurrentUserOwner && member.userId != group.ownerUserId
                        }
                    )
                }
                .ifEmpty {
                    listOf(
                        ListItemEmptySpaceData(100.dp2px),
                        EmptyStateData(
                            iconRes = R.drawable.ic_person_add,
                            infoText = myApp.getString(R.string.group_members_empty_info),
                            buttonText = myApp.getString(R.string.group_details_add_member_button)
                        )
                    )
                }
            GroupMemberUiModel(
                members = members,
                isMyGroup = group.isCurrentUserOwner
            )
        }
        .asLiveData()

    fun removeMember(member: MemberModel): LiveData<Payload<Unit>> {
        return groupRepo.removeMember(member)
            .mapToPayload { Unit }
            .asLiveData()
    }

}

data class GroupMemberUiModel(
    val members: List<ListItemType>,
    val isMyGroup: Boolean
)