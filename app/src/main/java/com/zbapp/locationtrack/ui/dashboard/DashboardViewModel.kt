package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.zbapp.location.repo.LocationSharingManager
import com.zbapp.location.repo.circle.CircleRepository
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.ui.shared.list.ListSubtitleItemData
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
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

    fun stopTracking() {
        locationInit.stopLocationUpdates()
        locationSharing.stopAllSharing()
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

                if(!location.isOK()) {
                    list.add(ListItemEmptySpaceData(16.dp2px))
                }
            }

            val active = groups.filter { group -> sharing.any { it.circleId == group.id } }.map { group ->
                GroupItemData(group, active = true)
            }

            val inactive = groups.filter { group -> sharing.none { it.circleId == group.id } }.map { group ->
                GroupItemData(group, active = false)
            }

            if(active.isEmpty()) {
                list.addAll(inactive)
            } else {
                list.add(ListSubtitleItemData(AppClass.context.getString(R.string.dashboard_groups_section_active)))
                list.addAll(active)
                if(inactive.isNotEmpty()) {
                    list.add(ListSubtitleItemData(AppClass.context.getString(R.string.dashboard_groups_section_inactive)))
                    list.addAll(inactive)
                }
            }

            list.add(GroupAddData())

            list
        }
            .asLiveData()
    }
}