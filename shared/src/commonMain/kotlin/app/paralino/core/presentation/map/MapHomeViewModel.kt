package app.paralino.core.presentation.map

import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MapHomeViewModel constructor(
    private val sharingManager: LocationSharingManager,
    private val groupRepo: UserGroupLocalDataSource,
    private val placesRepo: PlacesRepository,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val realtimeEvents: RealtimeEventsMaker,
    private val userRepository: UserRepository,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val appPrefs: AppPrefs,
    private val deviceApiProvider: IDeviceApiProvider,
    private val valueFormatter: IValueFormatter,
    private val remoteConfig: RemoteConfigRepository,
    private val appRatingManager: AppRatingManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(
        MapHomeScreenState.Empty(
            currentUserImage = userRepository.localUser.value.data?.data?.userImage
                ?: ImageModel.emoji(Emojis.robot)
        )
    )
    val uiState: StateFlow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val uiFocusedStatus = MutableStateFlow(false)
    private val groupIdData = MutableStateFlow("".asGroupOrSpaceId())
    private val uiRefreshPingFlow = MutableStateFlow<Instant?>(null)

    val allGroups: StateFlow<Resource<List<UserGroupWithMetadata>>> =
        groupRepo.allGroupsState

    private var groupSubscriptionJob: Job = SupervisorJob()
    private val locationRequestDuration = 20.seconds
    private val locationRequestDelay = 1.minutes

    // Initialize managers
    private val mapStateManager = MapStateManager(messageDispatcher)
    private val groupStateManager = GroupStateManager(
        groupRepo = groupRepo,
        personalSpaceRepo = personalSpaceRepo,
        userRepository = userRepository,
        sharingManager = sharingManager,
        appPrefs = appPrefs,
        remoteConfig = remoteConfig,
        messageDispatcher = messageDispatcher,
        coroutineScope = coroutineScope,
        allGroups = allGroups,
        groupIdData = groupIdData,
        eventChannel = _eventChannel
    )
    private val memberActionHandler = MemberActionHandler(
        clearUserLocation = clearUserLocation,
        locationRequestUseCase = locationRequestUseCase,
        sharingManager = sharingManager,
        appPrefs = appPrefs,
        messageDispatcher = messageDispatcher,
        coroutineScope = coroutineScope,
        eventChannel = _eventChannel,
        locationRequestDuration = locationRequestDuration
    )
    private val locationFocusManager = LocationFocusManager(
        locationRequestUseCase = locationRequestUseCase,
        realtimeEvents = realtimeEvents,
        sharingManager = sharingManager,
        deviceApiProvider = deviceApiProvider,
        coroutineScope = coroutineScope,
        uiRefreshPingFlow = uiRefreshPingFlow,
        uiFocusedStatus = uiFocusedStatus,
        groupIdData = groupIdData
    )
    private val mapStateMapper = MapStateMapper(
        appPrefs = appPrefs,
        valueFormatter = valueFormatter,
        remoteConfig = remoteConfig,
        memberActionHandler = memberActionHandler,
        locationRequestDuration = locationRequestDuration,
        locationRequestDelay = locationRequestDelay
    )
    private val mapEventHandler = MapEventHandler(
        mapStateManager = mapStateManager,
        groupStateManager = groupStateManager,
        memberActionHandler = memberActionHandler,
        locationFocusManager = locationFocusManager,
        uiState = uiState,
        eventChannel = _eventChannel,
        appPrefs = appPrefs,
        uiFocusedStatus = uiFocusedStatus,
        updateUiState = { state -> _uiState.value = state },
        updateReadyState = ::updateReadyState,
        coroutineScope = coroutineScope
    )

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.MAIN_MAP)

        setupGroupValidation()
        setupAppLifecycleHandling()
        setupLocationFocusManagement()
        setupStateBuilding()
    }

    private fun setupGroupValidation() {
        allGroups.onEach { groupsData ->
            val selectedGroup = groupIdData.value.takeIf { it.id.isNotBlank() }
                ?: appPrefs.selectedGroupId?.asGroupOrSpaceId()
            if (groupsData is Resource.Ready && groupsData.data.isNotEmpty() && !selectedGroup?.id.isNullOrBlank() && selectedGroup.isGroup) {
                if (groupsData.data.none { it.id == selectedGroup.id }) {
                    mapEventHandler.onEvent(
                        MapHomeScreenUiEvent.SwitchGroup(
                            groupId = groupsData.data.firstOrNull()?.id?.asGroupOrSpaceId()
                        )
                    )
                }
            }
        }.launchIn(coroutineScope)
    }

    private fun setupAppLifecycleHandling() {
        combine(
            deviceApiProvider.appLifecycleState,
            uiFocusedStatus
        ) { appLifecycleState, uiFocused ->
            locationFocusManager.keepUiRefreshPing()
            if (appLifecycleState.comingFromBackground && uiFocused) {
                if (groupIdData.value.id.isNotBlank()) {
                    delay(2000)
                    logg.v(
                        "home_screen",
                        "App in foreground, try to refresh selected group ${groupIdData.value}"
                    )
                    coroutineScope.launch(Dispatchers.IO) {
                        if (groupIdData.value.isPersonalSpace) {
                            personalSpaceRefresh.refreshPersonalSpace(10.seconds.inWholeMilliseconds)
                        } else {
                            groupPreRefresh.refreshGroupWithMembers(groupIdData.value.id)
                        }
                    }
                } else {
                    logg.v(
                        "home_screen",
                        "App in foreground but no selected group - try to select default group"
                    )
                    mapEventHandler.onEvent(MapHomeScreenUiEvent.SwitchGroup(null))
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun setupLocationFocusManagement() {
        combine(
            groupIdData,
            deviceApiProvider.appLifecycleState
                .map { it.current.activeOrInactive }
                .distinctUntilChanged(),
            uiFocusedStatus,
            uiState.map { it.asReady()?.mapHomeScreenFocusType }
                .filterNotNull()
                .distinctUntilChanged(),
        ) { groupId, appState, isUiFocused, mapFocusType ->
            MapScreenLifecycleData(
                groupId = groupId,
                isAppFocused = appState,
                isUiFocused = isUiFocused,
                mapFocusType = mapFocusType
            )
        }
            .debounce(2000)
            .distinctUntilChanged()
            .onEach {
                locationFocusManager.initRealtime()
                locationFocusManager.keepLocationFocusOn(focusType = it.mapFocusType)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun setupStateBuilding() {
        groupIdData
            .onEach {
                if (it.id.isNotBlank()) {
                    logg.v("home_screen", "Selected group changed, try to refresh group $it")
                    coroutineScope.launch(Dispatchers.IO) {
                        if (it.isPersonalSpace) {
                            personalSpaceRefresh.refreshPersonalSpace(10.seconds.inWholeMilliseconds)
                        } else {
                            groupPreRefresh.refreshGroupWithMembers(it.id)
                        }
                    }
                }
            }
            .flatMapLatest {
                if (it.id.isBlank()) {
                    userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                        .map {
                            MapHomeScreenState.Empty(
                                currentUserImage = it?.userImage ?: ImageModel.emoji(Emojis.robot)
                            )
                        }
                } else {
                    combine(
                        uiRefreshPingFlow.filterNotNull(),
                        // TODO handle personal space
                        groupRepo.getGroupWithMembers(it.id),
                        placesRepo.getPlacesForGroupLocally(it.id),
                        locationFocusManager.getSharingContract(),
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { _, group, places, sharingStatus, userData ->

                        if (group == null || userData == null) {
                            return@combine MapHomeScreenState.Empty(
                                currentUserImage = ImageModel.emoji(
                                    Emojis.robot
                                )
                            )
                        }

                        mapStateMapper.buildReadyState(
                            groupData = group,
                            places = places,
                            sharingStatus = sharingStatus,
                            userData = userData,
                            previousState = _uiState.value.asReady()
                                ?.takeIf { it.groupId == group.id },
                            isSatelliteMode = _uiState.value.isSatelliteMode,
                            alertDialog = _uiState.value.alertDialog
                        )
                    }
                        .flowOn(Dispatchers.IO)
                }
            }
            .onEach { _uiState.value = it }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: MapHomeScreenUiEvent) {
        mapEventHandler.onEvent(event)
    }

    fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        return groupStateManager.getGroupPickerState()
    }

    private fun updateReadyState(action: (MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) {
        _uiState.update {
            when (it) {
                is MapHomeScreenState.Empty -> it
                is MapHomeScreenState.Ready -> action(it)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupSubscriptionJob.cancel()
        locationFocusManager.cancelJobs()
    }
}