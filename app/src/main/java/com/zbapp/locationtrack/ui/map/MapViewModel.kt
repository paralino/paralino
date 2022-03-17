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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MapViewModel constructor(
    app: Application,
    groupId: String?,
    private val locationRepo: LocationRepository,
    private val groupRepo: GroupRepository
) : AndroidViewModel(app) {

    private val groupIdData = MutableStateFlow(groupId ?: "")
    val selectedGroup = MutableStateFlow<Payload<UserGroup>>(Payload.Loading)
    private var locationRefreshJob: Job? = null

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
                refreshLocations()
                locationRepo.getLocationsForGroup(it)
            }
            .mapToPayload { it }
            .onEach { selectedGroup.value = it }
            .launchIn(viewModelScope)
    }

    fun switchGroup(groupId: String) {
        groupIdData.value = groupId
    }

    private fun refreshLocations() {
        val groupId = groupIdData.value.takeIf { it.isNotBlank() } ?: return

        locationRefreshJob?.cancel()
        locationRefreshJob = locationRepo.locationRefresh(groupId)
            .mapToPayload { Unit }
            .launchIn(viewModelScope)
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

    override fun onCleared() {
        super.onCleared()
        locationRefreshJob?.cancel()
    }

}