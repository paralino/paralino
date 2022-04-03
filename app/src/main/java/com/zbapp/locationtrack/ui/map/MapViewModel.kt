package com.zbapp.locationtrack.ui.map

import android.app.Application
import androidx.lifecycle.*
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupWithMembers
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMemberItemData
import com.zbapp.locationtrack.utils.extensions.fromUTCtoMapSharingTime
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MapViewModel constructor(
    app: Application,
    groupId: String?,
    private val locationRepo: LocationRepository,
    private val groupRepo: GroupRepository
) : AndroidViewModel(app) {

    private val groupIdData = MutableStateFlow(groupId ?: "")

    val locationRefreshSubscription = groupIdData.filter { it.isNotBlank() }
        .flatMapLatest { locationRepo.locationRefresh(it).mapToPayload { Unit } }
        .asLiveData()

    val selectedGroup = MutableStateFlow<Payload<MapUiModel>>(Payload.Loading)

    init {

        if (groupId.isNullOrBlank()) {
            viewModelScope.launch {
                groupRepo.getAllGroupsLocally().firstOrNull()?.firstOrNull()?.let {
                    groupIdData.value = it.id
                }
            }
        }

        groupIdData
            .filter { it.isNotEmpty() }
            .flatMapLatest {
                locationRepo.getLocationsForGroup(it)
            }
            .map { group ->
                MapUiModel(
                    group = group,
                    memberListData = group.members.map { member ->
                        val actionItem = GroupMemberItemData.MemberAction.DELETE_LOCATION.takeIf {
                            member.userId == config.userId && member.mutableData != null
                        }
                        GroupMemberItemData(
                            member = member,
                            actionItem = actionItem,
                            uiType = selectedGroup.value.data?.memberListData?.firstOrNull { it.member.id == member.id }?.uiType
                                ?.takeIf { actionItem != null }
                                ?: GroupMemberItemData.MemberUiType.COLLAPSED
                        )
                    }
                )
            }
            .mapToPayload { it }
            .onEach { selectedGroup.value = it }
            .launchIn(viewModelScope)
    }

    fun switchGroup(groupId: String) {
        groupIdData.value = groupId
    }

    fun getMarkers(): LiveData<Payload<MapUiModel>> {
        return selectedGroup.asLiveData()
    }

    fun getSharingTime(): LiveData<String?> {
        return locationRepo.sharingManager.allContractsFlow.combine(groupIdData) { contracts, group ->
            contracts.data?.firstOrNull { it.groupId == group }
        }
            .map {
                it?.asValidContract()?.let {
                    it.endTimeMillisUTC?.toDateTimeUTC()?.fromUTCtoMapSharingTime()
                        ?: myApp.getString(R.string.groups_sharing_duration_type_infinite)
                }
            }
            .asLiveData()
    }

    fun getAllGroups(): LiveData<List<UserGroup>> {
        return liveData {
            emit(groupRepo.getAllGroupsLocally().firstOrNull() ?: emptyList())
        }
    }

    fun clearMyLocation(): LiveData<Payload<Unit>> {
        return locationRepo.clearLocation(groupIdData.value)
            .mapToPayload { it }
            .asLiveData()
    }

}

data class MapUiModel(
    val group: UserGroupWithMembers,
    val memberListData: List<GroupMemberItemData>
)