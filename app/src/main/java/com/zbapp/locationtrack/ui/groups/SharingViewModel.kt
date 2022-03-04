package com.zbapp.locationtrack.ui.groups

import android.app.Application
import androidx.lifecycle.*
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


class SharingViewModel(
    app: Application,
    private val args: SharingStartFragmentArgs?,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager
): AndroidViewModel(app) {

    private val selectedGroupLiveData = MutableLiveData(GroupItemData("", "Pick a group", Emojis.lightBulb, null))
    val selectedGroup: LiveData<GroupItemData> = selectedGroupLiveData

    val locationStatus = myApp.locationStatus.asLiveData()

    init {
        args?.groupId?.let {
            setSelectedGroup(it)
        }
    }

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

    fun startSharing(durationMillis: Long): LiveData<Boolean> {
        return selectedGroup.value?.id?.let {
            liveData {
                locationSharingManager.startSharing(
                    LocationSharingContract(it, LocationSharingContract.forDuration(durationMillis))
                )
                val active = withTimeoutOrNull(2000) {
                    locationSharingManager.isSharingActiveForGroup(it).filter { it }.firstOrNull()
                } ?: false
                emit(active)
            }
        } ?: liveData { emit(false) }
    }

}