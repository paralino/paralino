package com.zbapp.locationtrack.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zbapp.locationtrack.tracking.LocationInitHandler

class DashboardViewModel(
    val locationInit: LocationInitHandler
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    fun isTrackingActive(): Boolean {
        return locationInit.isTracking
    }

    fun startTracking() {

    }

    fun startSharing() {

    }

    fun stopTracking() {
        locationInit.stopLocationUpdates()
    }

    fun stopSharing() {

    }
}