package com.zbapp.locationtrack.ui.groups.detail.members

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.mapToPayload
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

    val groupMembers: LiveData<List<ListItemType>> = groupRepo.getGroupLocally(groupId)
        .map { group ->
            group.members
                .sortedBy { it.name }
                .sortedBy { it.userId == config.userId }
                .map {
                    GroupMemberItemData(
                        member = it,
                        canDelete = BuildConfig.internalTesting || (group.ownerId == config.userId && it.userId != config.userId)
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
        }
        .asLiveData()

    fun removeMember(member: MemberModel): LiveData<Payload<Unit>> {
        return groupRepo.removeMember(member)
            .mapToPayload { Unit }
            .asLiveData()
    }

}