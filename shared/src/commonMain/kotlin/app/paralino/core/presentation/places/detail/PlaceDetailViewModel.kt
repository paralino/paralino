package app.paralino.core.presentation.places.detail

import app.paralino.core.config
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlaceSettingsData
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
                    memberUserId = member.userId,
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
                groupName = group.name,
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

    fun toggleEnterSettings(memberId: String) {
        toggleSetting(memberId, isEnter = true) { setting ->
            GroupPlaceSettingsData(
                memberId = setting.memberId,
                memberUserId = setting.memberId,
                enter = !setting.notifyEnter,
                exit = setting.notifyExit
            )
        }
    }

    fun toggleExitSettings(memberId: String) {
        toggleSetting(memberId, isEnter = false) { setting ->
            GroupPlaceSettingsData(
                memberId = setting.memberId,
                memberUserId = setting.memberUserId,
                enter = setting.notifyEnter,
                exit = !setting.notifyExit
            )
        }
    }

    private fun toggleSetting(
        memberId: String,
        isEnter: Boolean,
        updateSetting: (PlaceSettingsState) -> GroupPlaceSettingsData
    ) {
        val state = _uiState.value.data ?: return
        val group = groupFlow.value.data ?: return
        if (state.fullLoading) return
        if (state.settings.any { it.loading }) return

        val setting = state.settings.firstOrNull { it.memberId == memberId } ?: return
        val newSetting = updateSetting(setting)
        val currentMember = group.memberByUserId(config.userId)

        placesRepo.updatePlaceSettings(state.placeId, currentMember.id, newSetting)
            .mapToPayload { it }
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapSuccess {
                        val newList = it.settings.toMutableList().apply {
                            removeAll { it.memberId == memberId }
                            add(
                                setting.copy(
                                    loadingEnter = result is Payload.Loading && isEnter,
                                    loadingExit = result is Payload.Loading && !isEnter
                                )
                            )
                        }
                        it.copy(settings = newList)
                    }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> {}
                    is Payload.Error -> {
                        _eventChannel.send(PlaceDetailScreenEvent.Error(result.exception))
                    }
                }
            }
            .launchIn(coroutineScope)
    }

}