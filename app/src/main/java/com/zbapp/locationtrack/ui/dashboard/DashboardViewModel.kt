package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.location.models.circle.Circle
import com.zbapp.location.repo.LocationSharingManager
import com.zbapp.location.repo.circle.CircleRepository
import com.zbapp.location.repo.location.LocationRepository
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.utils.ListItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class DashboardViewModel(
    val locationInit: LocationInitHandler,
    private val locationSharing: LocationSharingManager,
    private val circleRepository: CircleRepository,
    context: Context
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    val locationStatus = MutableStateFlow(LocationConditionStatus.getLocationStatus(context))

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

    fun getGroups(): LiveData<List<ListItemType>> {

        return combine(
            locationStatus,
            circleRepository.getAllCircles(),
            locationSharing.allContractsFlow
        ) { location, groups, sharing ->
            val list = mutableListOf<ListItemType>()

            if (locationSharing.isSharingActive) {
                if (!location.isGpsOn)
                    list.add(ErrorListItemType.GPS)
                if (!location.isPermissionAllowed)
                    list.add(ErrorListItemType.PERMISSION)
            }

            list.addAll(
                groups.map { group ->
                    GroupItemData(group, sharing.any { it.circleId == group.id })
                }
            )

            list
        }
            .asLiveData()
    }
}