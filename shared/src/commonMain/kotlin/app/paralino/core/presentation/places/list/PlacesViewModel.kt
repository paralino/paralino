package app.paralino.core.presentation.places.list

import app.paralino.core.models.Payload
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class PlacesViewModel(
    groupId: String,
    private val placesRepo: PlacesRepository,
    private val groupRepo: GroupRepository,
    scope: CoroutineScope
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<PlacesListScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PlacesListScreenState>> = _uiState

    private val _eventChannel = Channel<PlacesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            groupRepo.getGroupWithoutMembersLocally(groupId),
            placesRepo.getPlacesForGroupLocally(groupId)
        ) { group, places ->

            val placesState = places.map {
                PlaceListItemData(
                    id = it.id,
                    name = it.data.name,
                    image = it.data.image,
                    private = it.private,
                    lat = it.data.lat,
                    lng = it.data.lng,
                    radiusMeters = it.data.radiusMeters
                )
            }

            val state = PlacesListScreenState(
                groupId = groupId,
                groupName = group.name,
                places = placesState
            )
            state
        }
            .onEach { state -> _uiState.update { Payload.Success(state) } }
            .catch { _eventChannel.send(PlacesScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }
}