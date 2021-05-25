package com.zbapp.locationtrack.ui.groups

import androidx.lifecycle.*
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import com.zbapp.locationtrack.ui.dashboard.GroupListItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SharingViewModel(
    private val groupRepo: GroupRepository
): ViewModel() {

    private val selectedGroupLiveData = MutableLiveData(GroupItemData("", "Pick a group", Emojis.lightBulb, null))
    val selectedGroup: LiveData<GroupItemData> = selectedGroupLiveData

    fun getAllGroups(): LiveData<List<UserGroup>> {
        return groupRepo.getAllGroupsLocally().asLiveData()
    }

    fun setSelectedGroup(groupId: String) {
        viewModelScope.launch {
            groupRepo.getAllGroupsLocally().firstOrNull()?.firstOrNull { it.id == groupId }?.let {
                selectedGroupLiveData.postValue(
                    GroupItemData(it, GroupItemData.GroupUiType.INACTIVE)
                )
            }
        }
    }

}