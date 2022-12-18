package app.paralino.core.presentation.dashboard

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.extensions.combine
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val locationSharing: LocationSharingManager,
    private val groupRepository: GroupRepository,
    private val placesRepo: PlacesRepository,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val userRepo: UserRepository,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<DashboardScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<DashboardScreenState>> = _uiState

    private val _eventChannel = Channel<DashboardScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        refreshGroups()
        loadDashboard()
    }

    fun refreshGroups() {
        coroutineScope.launch {
            try {
                groupRepository.scheduleRefreshAllGroupsWithPlaces()
            } catch (e: Exception) { }
        }
    }

    private fun loadDashboard() {
        combine(
            userRepo.localUser,
            locationStatusProvider.status,
            groupRepository.getAllGroupsLocally(),
            placesRepo.hasAnyPlace(),
            locationSharing.allContractsFlow.map { it.data }.filterNotNull(),
            connectionStatus.status
        ) { user, location, groups, hasPlace, sharing, network ->

            val active =
                groups.filter { group -> sharing.any { it.groupId == group.id } }.map { group ->
                    GroupItemData(group, uiType = GroupItemData.GroupUiType.ACTIVE)
                }

            val inactive =
                groups.filter { group -> sharing.none { it.groupId == group.id } }.map { group ->
                    GroupItemData(group, uiType = GroupItemData.GroupUiType.ACTIVE)
                }

            val errors = buildList {
                if(sharing.isNotEmpty() && !network.isOnline)
                    add(ConditionErrorType.NETWORK)

                sharing
                    .takeIf { it.isNotEmpty() }
                    ?.let { location.getPossibleErrors(forBackground = it.any { it.isLongTerm }) }
                    .let { it ?: emptyList() }
                    .let {
                        addAll(it)
                    }

                if(hasPlace && !location.isNotificationPermissionAllowed) {
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
                }

                user.data?.needsEmailVerification?.takeIf { it }?.let {
                    add(ConditionErrorType.EMAIL_VERIFICATION)
                }
            }

            DashboardScreenState(
                email = user.data?.email ?: "",
                activeGroups = active,
                inactiveGroups = inactive,
                errors = errors
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

    fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                _eventChannel.send(DashboardScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                _eventChannel.send(DashboardScreenEvent.Error(e))
            }
        }
    }

    fun stopTracking(groupId: String) {
        coroutineScope.launch {
            locationSharing.stopSharing(groupId)
        }
    }

}