package app.paralino.core.presentation.places.list

import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlacesViewModel(
    groupId: String,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val placesRepo: PlacesRepository,
    private val groupRepo: UserGroupLocalDataSource,
    val locationStatusProvider: ILocationStatusProvider,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<PlacesListScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PlacesListScreenState>> = _uiState

    private val _eventChannel = Channel<PlacesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            groupRepo.getGroupWithoutMembers(groupId),
            placesRepo.getAndRefreshPlacesForGroup(groupId),
            locationStatusProvider.status
        ) { group, places, status ->

            val placesState = places.map {
                PlaceListItemData(it)
            }

            val errors = if (places.isNotEmpty() && !status.isNotificationPermissionAllowed) {
                listOf(ConditionErrorType.PERMISSION_NOTIFICATIONS)
            } else {
                emptyList()
            }

            val placesLimit = group.placesLimit(remoteConfigRepo.configState.value)
            val state = PlacesListScreenState(
                groupId = groupId,
                groupImage = group.image,
                groupName = group.name,
                placesLimit = placesLimit,
                places = placesState,
                canShowPlacesLimit = if (group.isCurrentUserOwner) {
                    true
                } else {
                    // only show places limit to other members if close to limit
                    placesState.size >= placesLimit.minus(2)
                },
                shouldShowPlanBanner = group.isCurrentUserOwner && !group.isGroupPremium(config = remoteConfigRepo.configState.value),
                errors = errors
            )
            state
        }
            .onEach { state -> _uiState.update { Payload.Success(state) } }
            .catch { _eventChannel.send(PlacesScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

    fun tryCreatePlace() {
        val state = _uiState.value.data ?: return
        coroutineScope.launch {
            if (state.places.size >= state.placesLimit)
                _eventChannel.send(PlacesScreenEvent.GroupPlacesLimitReached(state.placesLimit))
            else
                _eventChannel.send(PlacesScreenEvent.AddPlace)
        }
    }
}