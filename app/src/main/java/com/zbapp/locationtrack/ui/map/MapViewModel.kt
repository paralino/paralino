package com.zbapp.locationtrack.ui.map

import android.app.Application
import androidx.lifecycle.*
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.utils.extensions.mapToPayload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.myApp
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

    val selectedGroup = MutableStateFlow<Payload<UserGroup>>(Payload.Loading)

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
            .mapToPayload { it }
            .onEach { selectedGroup.value = it }
            .launchIn(viewModelScope)
    }

    fun switchGroup(groupId: String) {
        groupIdData.value = groupId
    }

    fun getMarkers(): LiveData<Payload<UserGroup>> {
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

}