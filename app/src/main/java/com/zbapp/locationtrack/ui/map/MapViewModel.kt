package com.zbapp.locationtrack.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.LocationRepository


class MapViewModel(
    val locationRepository: LocationRepository
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    fun getMarkers(): LiveData<List<BaseLocationModel>> {
        return locationRepository.getLocationsForGroup(UserGroup.personalTracking.id).asLiveData()
    }

}