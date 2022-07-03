package app.paralino.core.presentation.dashboard

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val locationStatusProvider: ILocationStatusProvider,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<DashboardScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<DashboardScreenState>> = _uiState

    private val _eventChannel = Channel<DashboardScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

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
                errors = sharing.takeIf { it.isNotEmpty() }?.let {
                    location.getPossibleErrors(forBackground = it.any { it.isLongTerm })
                } ?: emptyList()
            )
        }
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { result }
                if (result is Payload.Error) {
                    _eventChannel.send(DashboardScreenEvent.Error(result.exception))
                }
            }
            .launchIn(coroutineScope)
    }


    fun stopTracking(groupId: String) {
        coroutineScope.launch {
            locationSharing.stopSharing(groupId)
        }
    }

}