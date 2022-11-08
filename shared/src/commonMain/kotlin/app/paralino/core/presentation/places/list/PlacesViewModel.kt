package app.paralino.core.presentation.places.list

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.ILocationStatusProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class PlacesViewModel(
    groupId: String,
    private val placesRepo: PlacesRepository,
    private val groupRepo: GroupRepository,
    private val locationStatusProvider: ILocationStatusProvider,
    scope: CoroutineScope
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<PlacesListScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PlacesListScreenState>> = _uiState

    private val _eventChannel = Channel<PlacesScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            groupRepo.getGroupWithoutMembersLocally(groupId),
            placesRepo.getAndRefreshPlacesForGroup(groupId),
            locationStatusProvider.status
        ) { group, places, status ->

            val placesState = places.map {
                PlaceListItemData(
                    id = it.id,
                    name = it.data.data?.name ?: "---",
                    image = it.data.data?.image ?: ImageModel.emoji(Emojis.robot),
                    private = it.private,
                    lat = it.data.data?.lat,
                    lng = it.data.data?.lng,
                    radiusMeters = it.data.data?.radiusMeters ?: 0
                )
            }

            val errors = if(places.isNotEmpty() && !status.isNotificationPermissionAllowed) {
                listOf(ConditionErrorType.PERMISSION_NOTIFICATIONS)
            } else {
                emptyList()
            }

            val state = PlacesListScreenState(
                groupId = groupId,
                groupName = group.name,
                places = placesState,
                errors = errors
            )
            state
        }
            .onEach { state -> _uiState.update { Payload.Success(state) } }
            .catch { _eventChannel.send(PlacesScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }
}