package com.zbapp.locationtrack.ui.dashboard

import android.app.Application
import androidx.lifecycle.*
import com.zbapp.core.logg
import com.zbapp.core.models.data
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.shared.list.EmptyStateData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpaceData
import com.zbapp.locationtrack.ui.shared.list.ListSubtitleItemData
import com.zbapp.locationtrack.utils.extensions.dp2px
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    app: Application,
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val loginManager: LoginManager,
    private val locationStatusProvider: ILocationStatusProvider
) : AndroidViewModel(app) {

    val locationStatus = locationStatusProvider.status

    fun isSharingActive(): Boolean {
        return locationSharing.isSharingActive()
    }

    fun stopAllTracking() {
        viewModelScope.launch {
            locationSharing.stopAllSharing()
        }
    }

    fun stopTracking(groupId: String) {
        viewModelScope.launch {
            locationSharing.stopSharing(groupId)
        }
    }

    fun getGroups(): LiveData<List<Any>> {

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
                emit(listOf(ConditionErrorType.EMAIL_VERIFICATION))
            }
        }

        return combine(
            locationStatus,
            groupRepository.getAllGroups(),
            locationSharing.allContractsFlow.map { it.data }.filterNotNull()
        ) { location, groups, sharing ->
            val list = mutableListOf<Any>()

            if (isSharingActive()) {
                list.addAll(location.getPossibleErrors(forBackground = sharing.any { it.isLongTerm }))
                if(!location.isOK()) {
                    list.add(ListItemEmptySpaceData(16.dp2px))
                }
            }

            val active = groups.filter { group -> sharing.any { it.groupId == group.id } }.map { group ->
                GroupItemData(group, uiType = GroupItemData.GroupUiType.ACTIVE)
            }

            val inactive = groups.filter { group -> sharing.none { it.groupId == group.id } }.map { group ->
                GroupItemData(group, uiType = GroupItemData.GroupUiType.INACTIVE)
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