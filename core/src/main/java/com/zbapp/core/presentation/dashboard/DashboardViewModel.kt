package com.zbapp.core.presentation.dashboard

import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val locationStatusProvider: ILocationStatusProvider,
    scope: CoroutineScope
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<DashboardScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<DashboardScreenState>> = _uiState

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        combine(
            locationStatusProvider.status,
            groupRepository.getAllGroups(),
            locationSharing.allContractsFlow.map { it.data }.filterNotNull()
        ) { location, groups, sharing ->

            val active =
                groups.filter { group -> sharing.any { it.groupId == group.id } }.map { group ->
                    GroupItemData(group, uiType = GroupItemData.GroupUiType.ACTIVE)
                }

            val inactive =
                groups.filter { group -> sharing.none { it.groupId == group.id } }.map { group ->
                    GroupItemData(group, uiType = GroupItemData.GroupUiType.ACTIVE)
                }

            DashboardScreenState(
                activeGroups = active,
                inactiveGroups = inactive,
                errors = location.getPossibleErrors(forBackground = sharing.any { it.isLongTerm })
            )
        }
            .mapToPayload { it }
            .onEach { _uiState.value = it }
            .launchIn(coroutineScope)
    }


    fun stopTracking(groupId: String) {
        coroutineScope.launch {
            locationSharing.stopSharing(groupId)
        }
    }

}