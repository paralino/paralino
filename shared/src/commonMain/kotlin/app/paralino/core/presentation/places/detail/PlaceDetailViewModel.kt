package app.paralino.core.presentation.places.detail

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.PlaceAlertsSettingsUpdateUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.places.GroupPlaceSettingsEntry
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaceDetailViewModel(
    val groupId: String,
    val placeId: String,
    private val placesRepo: PlacesRepository,
    private val updatePlaceAlerts: PlaceAlertsSettingsUpdateUseCase,
    private val groupRepo: UserGroupLocalDataSource,
    private val cryptoCore: CryptoCore,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val placeFlow =
        MutableStateFlow<Resource<GroupPlaceWithSettings>>(Resource.NotInitialized)
    private val groupFlow =
        MutableStateFlow<Resource<UserGroupWithMembers>>(Resource.NotInitialized)
    private val _uiState = MutableStateFlow<Payload<PlaceDetailScreenState>>(Payload.Loading)
    val uiState: StateFlow<Payload<PlaceDetailScreenState>> = _uiState

    private val _eventChannel = Channel<PlaceDetailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()


    init {
        combine(
            groupRepo.getGroupWithMembers(groupId).filterNotNull(),
            placesRepo.getPlaceWithSettingsLocally(placeId)
        ) { group, place ->

            placeFlow.update { Resource.Ready(place) }
            groupFlow.update { Resource.Ready(group) }

            val settings = group.members.map { member ->
                val memberSetting = place.settings?.alertData?.settings?.firstOrNull {
                    it.memberUserId == member.member.userId
                }
                PlaceSettingsState(
                    groupId = groupId,
                    placeId = placeId,
                    memberUserId = member.member.userId,
                    isCurrentUser = member.member.userId == appPrefs.userId,
                    name = member.member.name,
                    image = member.member.image ?: ImageModel.emoji(Emojis.robot),
                    // default to true for other members and false to current user
                    notifyEnter = memberSetting?.events?.get(enterEvent() ?: "")
                        ?: (member.member.userId != appPrefs.userId),
                    notifyExit = memberSetting?.events?.get(exitEvent() ?: "")
                        ?: (member.member.userId != appPrefs.userId),
                    loadingEnter = false,
                    loadingExit = false
                )
            }.sortedSettings()

            PlaceDetailScreenState(
                groupId = groupId,
                placeId = placeId,
                groupName = group.name,
                name = place.place.data.data?.name ?: "---",
                image = place.place.data.data?.image ?: ImageModel.emoji(Emojis.lock),
                private = place.place.private,
                lat = place.place.data.data?.lat,
                lng = place.place.data.data?.lng,
                radiusMeters = place.place.data.data?.radiusMeters ?: 0,
                canHaveAlerts = place.place.canSendAlert,
                canEdit = canEditPlace(),
                fullLoading = false,
                arePlaceAlertsActive = place.settings?.alertData?.isActive != false,
                isPlaceAlertsSwitchLoading = false,
                settings = settings
            )
        }
            .mapToPayload { it }
            .onEach { result -> _uiState.update { result } }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        coroutineScope.launch {
            try {
                placesRepo.refreshPlaceWithSettingsMaybe(placeId = placeId)
            } catch (e: Exception) {
                logg.w("places", "Failed to refresh place: ${e.message}")
            }
        }
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
        return group.isCurrentUserOwner || place.place.ownerUserId == appPrefs.userId
    }

    fun togglePlaceAlertsSettings(revokeItemState: Boolean) {
        val state = _uiState.value.data ?: return
        if (state.fullLoading) return
        if (state.isPlaceAlertsSwitchLoading) return
        if (state.settings.any { it.loading }) return
        if (updatePlaceAlerts.isProcessingAny()) return

        val newActiveState = !state.arePlaceAlertsActive
        val newSettings = if (revokeItemState) {
            state.settings.map {
                GroupPlaceSettingsEntry(
                    memberUserId = it.memberUserId,
                    events = buildEvents(
                        enter = newActiveState,
                        exit = newActiveState
                    )
                )
            }
        } else {
            null
        }

        updatePlaceAlerts.updatePlaceAlertsSettings(
            placeId = placeId,
            active = newActiveState,
            settings = newSettings
        )
            .mapToPayload { it }
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapSuccess {
                        it.copy(isPlaceAlertsSwitchLoading = result is Payload.Loading)
                    }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> {}
                    is Payload.Error -> {
                        messageDispatcher.showSnackbar(result.exception.errorString())
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun toggleEnterSettings(userId: String) {
        toggleSetting(userId, isEnter = true) { setting ->
            GroupPlaceSettingsEntry(
                memberUserId = setting.memberUserId,
                events = buildEvents(enter = !setting.notifyEnter, exit = setting.notifyExit)
            )
        }
    }

    fun toggleExitSettings(userId: String) {
        toggleSetting(userId, isEnter = false) { setting ->
            GroupPlaceSettingsEntry(
                memberUserId = setting.memberUserId,
                events = buildEvents(enter = setting.notifyEnter, exit = !setting.notifyExit)
            )
        }
    }

    private fun toggleSetting(
        userId: String,
        isEnter: Boolean,
        updateSetting: (PlaceSettingsState) -> GroupPlaceSettingsEntry
    ) {
        val state = _uiState.value.data ?: return
        if (state.fullLoading) return
        if (state.isPlaceAlertsSwitchLoading) return
        if (!state.arePlaceAlertsActive) return
        if (state.settings.any { it.loading }) return
        if (updatePlaceAlerts.isProcessingAny()) return

        val setting = state.settings.firstOrNull { it.memberUserId == userId } ?: return
        val newSetting = updateSetting(setting)

        updatePlaceAlerts.updatePlaceAlertsSettings(placeId = placeId, data = newSetting)
            .mapToPayload { it }
            .debounce(100)
            .onEach { result ->
                _uiState.update {
                    it.mapSuccess {
                        val newList = it.settings.toMutableList().apply {
                            removeAll { it.memberUserId == userId }
                            add(
                                setting.copy(
                                    loadingEnter = result is Payload.Loading && isEnter,
                                    loadingExit = result is Payload.Loading && !isEnter
                                )
                            )
                        }.sortedSettings()
                        it.copy(settings = newList)
                    }
                }
                when (result) {
                    Payload.Loading -> {}
                    is Payload.Success -> {}
                    is Payload.Error -> {
                        messageDispatcher.showSnackbar(result.exception.errorString())
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun List<PlaceSettingsState>.sortedSettings(): List<PlaceSettingsState> {
        return this.sortedBy { it.name }
            .sortedBy { it.memberUserId == appPrefs.userId }
    }

    private fun buildEvents(
        enter: Boolean,
        exit: Boolean
    ): Map<String, Boolean> {
        val enterEvent = enterEvent() ?: return emptyMap()
        val exitEvent = exitEvent() ?: return emptyMap()
        return buildMap {
            put(enterEvent, enter)
            put(exitEvent, exit)
        }
    }

    private fun enterEvent(): String? {
        val eventSeed = placeFlow.value.data?.place?.eventSeed ?: return null
        return cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true)
    }

    private fun exitEvent(): String? {
        val eventSeed = placeFlow.value.data?.place?.eventSeed ?: return null
        return cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false)
    }
}