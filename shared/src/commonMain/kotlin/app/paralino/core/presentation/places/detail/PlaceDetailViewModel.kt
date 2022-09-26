package app.paralino.core.presentation.places.detail

import app.paralino.core.config
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaceDetailViewModel(
    groupId: String,
    placeId: String,
    private val placesRepo: PlacesRepository,
    private val groupRepo: GroupRepository,
    scope: CoroutineScope
) : CoreViewModel(scope) {

    private val placeFlow =
        MutableStateFlow<Resource<GroupPlaceWithSettings>>(Resource.NotInitialized)
    private val groupFlow =
        MutableStateFlow<Resource<UserGroupWithMembers>>(Resource.NotInitialized)
    private val _uiState = MutableStateFlow<Payload<PlaceDetailScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<PlaceDetailScreenState>> = _uiState

    private val _eventChannel = Channel<PlaceDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    init {
        combine(
            groupRepo.getGroupWithMembersLocally(groupId),
            placesRepo.getPlaceWithSettings(placeId)
        ) { group, place ->

            placeFlow.update { Resource.Ready(place) }
            groupFlow.update { Resource.Ready(group) }

            val settings = group.members.map { member ->
                val memberSetting =
                    place.settings?.alertData?.firstOrNull { it.memberId == member.id }
                PlaceSettingsState(
                    groupId = groupId,
                    placeId = placeId,
                    memberId = member.id,
                    name = member.name,
                    image = member.image ?: ImageModel.emoji(Emojis.robot),
                    // default to true for other members and false to current user
                    notifyEnter = memberSetting?.enter ?: (member.userId != config.userId),
                    notifyExit = memberSetting?.exit ?: (member.userId != config.userId),
                    loadingEnter = false,
                    loadingExit = false
                )
            }

            PlaceDetailScreenState(
                groupId = groupId,
                placeId = placeId,
                name = place.place.data.name,
                image = place.place.data.image,
                private = place.place.private,
                lat = place.place.data.lat,
                lng = place.place.data.lng,
                radiusMeters = place.place.data.radiusMeters,
                canEdit = canEditPlace(),
                fullLoading = false,
                settings = settings
            )
        }
            .mapToPayload { it }
            .onEach { result -> _uiState.update { result } }
            .launchIn(coroutineScope)
    }

    fun deletePlace() {

        val place = placeFlow.value.data

        if (place == null) {
            coroutineScope.launch {
                _eventChannel.send(PlaceDetailScreenEvent.Error(Throwable("Missing place")))
            }
            return
        }

        if (canEditPlace()) {
            placesRepo.deletePlace(place.place.id)
                .mapToPayload { it }
                .onEach { result ->
                    when (result) {
                        Payload.Loading -> {}
                        is Payload.Success -> _eventChannel.send(PlaceDetailScreenEvent.PlaceDeleted)
                        is Payload.Error -> _eventChannel.send(
                            PlaceDetailScreenEvent.Error(result.exception)
                        )
                    }
                    _uiState.update { it.mapSuccess { it.copy(fullLoading = result is Payload.Loading) } }
                }
                .launchIn(coroutineScope)
        } else {
            coroutineScope.launch {
                _eventChannel.send(PlaceDetailScreenEvent.Error(Throwable("Missing permission")))
            }
        }
    }

    private fun canEditPlace(): Boolean {
        val place = placeFlow.value.data ?: return false
        val group = groupFlow.value.data ?: return false
        return group.isCurrentUserOwner || place.place.ownerUserId == config.userId
    }

    fun updateEnterSettings(memberId: String, notifyEnter: Boolean) {

    }

    fun updateExitSettings(memberId: String, notifyExit: Boolean) {

    }

}