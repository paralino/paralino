package com.zbapp.locationtrack.ui.dashboard

import android.content.Context
import androidx.lifecycle.*
import com.zbapp.core.logg
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.ui.shared.list.EmptyStateData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptyState
import com.zbapp.locationtrack.ui.shared.list.ListSubtitleItemData
import com.zbapp.locationtrack.utils.ListItemType
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.toUTCString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class DashboardViewModel(
    val locationInit: LocationInitHandler,
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val loginManager: LoginManager,
    context: Context
) : ViewModel() {

    val locationStatus = MutableStateFlow(LocationConditionStatus.getLocationStatus(context))

    fun isTrackingActive(): Boolean {
        return locationInit.isTracking
    }

    fun startTracking() {
        viewModelScope.launch {
            val max = LocalDateTime.now().plusMinutes(2).toUTCString()
            locationSharing.startSharing(LocationSharingContract(UserGroup.personalTracking.id, max))
        }
    }

    fun stopTracking() {
        locationInit.stopLocationUpdates()
        viewModelScope.launch {
            locationSharing.stopAllSharing()
        }
    }

    fun deleteGroup(id: String) {
        groupRepository.deleteGroup(id)
            .catch {  }
            .launchIn(viewModelScope)
    }

    fun getGroups(): LiveData<List<ListItemType>> {

        if(!loginManager.isLoggedIn()) {
            return liveData {
                emit(listOf(
                    ListItemEmptySpaceData(100.dp2px),
                    EmptyStateData(
                        iconRes = null,
                        infoText = AppClass.context.getString(R.string.dashboard_info_logged_out),
                        buttonText = AppClass.context.getString(R.string.login_button_login)
                    )
                ))
            }
        } else if(!loginManager.isVerified()) {
            return liveData {
                emit(listOf(ErrorListItemType.EMAIL))
            }
        }

        return combine(
            locationStatus,
            groupRepository.getAllGroups(),
            locationSharing.allContractsFlow
        ) { location, groups, sharing ->
            val list = mutableListOf<ListItemType>()

            if (locationSharing.isSharingActive()) {
                if (!location.isGpsOn)
                    list.add(ErrorListItemType.GPS)
                if (!location.isPermissionAllowed)
                    list.add(ErrorListItemType.PERMISSION)

                if(!location.isOK()) {
                    list.add(ListItemEmptySpaceData(16.dp2px))
                }
            }

            val active = groups.filter { group -> sharing.any { it.groupId == group.id } }.map { group ->
                GroupItemData(group, active = true)
            }

            val inactive = groups.filter { group -> sharing.none { it.groupId == group.id } }.map { group ->
                GroupItemData(group, active = false)
            }

            if(active.isEmpty() && inactive.isEmpty()) {
                list.add(ListItemEmptySpaceData(100.dp2px))
                list.add(
                    EmptyStateData(
                        iconRes = R.drawable.ic_groups,
                        infoText = AppClass.context.getString(R.string.dashboard_info_no_groups)
                    )
                )
            } else if(active.isEmpty()) {
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
            .catch { logg.e("Error on dashboard", it) }
            .asLiveData()
    }
}