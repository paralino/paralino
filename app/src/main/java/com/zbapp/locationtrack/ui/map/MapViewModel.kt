package com.zbapp.locationtrack.ui.map

import androidx.lifecycle.*
import com.zbapp.core.models.Payload
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MapViewModel constructor(
    groupId: String?,
    private val locationRepo: LocationRepository,
    private val groupRepo: GroupRepository
) : ViewModel() {

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

    fun getAllGroups(): LiveData<List<UserGroup>> {
        return liveData {
            emit(groupRepo.getAllGroupsLocally().firstOrNull() ?: emptyList())
        }
    }

}