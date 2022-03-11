package com.zbapp.locationtrack.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class MapViewModel(
    val locationRepository: LocationRepository
) : ViewModel() {

    private val groupIdData = MutableStateFlow("")
    private var locationRefreshJob: Job? = null

    init {
        groupIdData
            .onEach { refreshLocations() }
            .launchIn(viewModelScope)
    }

    fun switchGroup(groupId: String) {
        groupIdData.value = groupId
    }

    private fun refreshLocations() {
        val groupId = groupIdData.value.takeIf { it.isNotBlank() } ?: return

        locationRefreshJob?.cancel()
        locationRefreshJob = locationRepository.locationRefresh(groupId)
            .mapToPayload { Unit }
            .launchIn(viewModelScope)
    }

    fun getMarkers(): LiveData<List<MemberModel>> {
        return locationRepository.getLocationsForGroup(groupIdData.value).asLiveData()
    }

}