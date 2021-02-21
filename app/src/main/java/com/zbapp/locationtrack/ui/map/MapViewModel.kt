package com.zbapp.locationtrack.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.repo.location.LocationRepository
import kotlinx.coroutines.flow.Flow


class MapViewModel(
    val locationRepository: LocationRepository
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    fun getMarkers(): LiveData<List<BaseLocationModel>> {
        return locationRepository.getLocationsForCircle(Circle.personalTracking.id).asLiveData()
    }

}