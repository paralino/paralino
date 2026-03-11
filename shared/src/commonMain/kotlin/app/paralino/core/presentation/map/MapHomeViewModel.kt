package app.paralino.core.presentation.map

import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.base.GroupWithMembersOrUserSpace
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.history.LocationHistoryViewModelController
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.RealtimeSubscriptionManager
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.personal_code.PersonalCodesRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MapHomeViewModel constructor(
    private val sharingManager: LocationSharingManager,
    private val groupRepo: UserGroupLocalDataSource,
    private val userRepository: UserRepository,
    private val personalCodeRepo: PersonalCodesRepository,
    private val placesRepo: PlacesRepository,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val realtimeManager: RealtimeSubscriptionManager,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
    private val geofenceManager: IGeofenceManager,
    private val appPrefs: AppPrefs,
    val deviceApiProvider: IDeviceApiProvider,
    private val valueFormatter: IValueFormatter,
    private val remoteConfig: RemoteConfigRepository,
    private val appConfigRepo: AppConfigRepository,
    private val appRatingManager: AppRatingManager,
    private val premiumStatusManager: PremiumStatusManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(
        MapHomeScreenState.Empty(
            groupsEmptyState = null,
            selectedGroupId = appPrefs.selectedGroupId,
            selectedGroupImage = null,
            isPersonalSpace = appPrefs.isPersonalSpaceSelected,
            currentUserImage = userRepository.localUser.value.data?.data?.userImage
                ?: ImageModel.emoji(Emojis.robot),
            androidMapSDKProvider = premiumStatusManager.defaultMapProvider()
        )
    )
    val uiState: StateFlow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val uiFocusedStatus = MutableStateFlow(false)
    private val groupIdData = MutableStateFlow(
        if (appPrefs.isPersonalSpaceSelected) {
            UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
        } else {
            (appPrefs.selectedGroupId ?: "").asGroupOrSpaceId()
        }
    )
    private val uiRefreshPingFlow = MutableStateFlow<Instant?>(null)

    val allGroups: StateFlow<Resource<List<UserGroupWithMetadata>>> =
        groupRepo.allGroupsState

    private var groupSubscriptionJob: Job = SupervisorJob()
    private val locationRequestDuration = 20.seconds
    private val locationRequestDelay = 25.seconds

    // Initialize managers
    private val mapStateManager = MapStateManager(
        messageDispatcher = messageDispatcher,
        coroutineScope = coroutineScope,
        eventChannel = _eventChannel
    )
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
        uiState = uiState,
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
        realtimeManager = realtimeManager,
        sharingManager = sharingManager,
        deviceApiProvider = deviceApiProvider,
        coroutineScope = coroutineScope,
        uiRefreshPingFlow = uiRefreshPingFlow,
        uiFocusedStatus = uiFocusedStatus,
        groupIdData = groupIdData
    )
    private val mapStateMapper = MapStateMapper(
        appPrefs = appPrefs,
        userGroupLocal = groupRepo,
        valueFormatter = valueFormatter,
        remoteConfig = remoteConfig,
        memberActionHandler = memberActionHandler,
        locationRequestDuration = locationRequestDuration,
        locationRequestDelay = locationRequestDelay,
        premiumStatusManager = premiumStatusManager
    )
    private val groupsEmptyStateManager = GroupsInitEmptyStateManager(
        userRepo = userRepository,
        personalCodesRepo = personalCodeRepo,
        groupLocalDataSource = groupRepo,
        deviceApiProvider = deviceApiProvider,
        messageDispatcher = messageDispatcher,
        coroutineScope = coroutineScope,
        eventChannel = _eventChannel
    )
    private val mapEventHandler = MapEventHandler(
        mapStateManager = mapStateManager,
        groupStateManager = groupStateManager,
        memberActionHandler = memberActionHandler,
        locationFocusManager = locationFocusManager,
        groupEmptyStateManager = groupsEmptyStateManager,
        uiState = uiState,
        eventChannel = _eventChannel,
        appPrefs = appPrefs,
        groupLocal = groupRepo,
        messageDispatcher = messageDispatcher,
        uiFocusedStatus = uiFocusedStatus,
        updateUiState = { state -> _uiState.value = state },
        updateReadyState = ::updateReadyState,
        coroutineScope = coroutineScope
    )

    private val locationHistoryController = MutableStateFlow<LocationHistoryViewModelController?>(null)
    val historyUiState: StateFlow<LocationHistoryScreenUiState?> = locationHistoryController
        .flatMapLatest { it?.uiState ?: flowOf(null) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    val historyEvents: Flow<LocationHistoryScreenEvent?> = locationHistoryController
        .flatMapLatest { it?.events ?: flowOf(null) }

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.MAIN_MAP)

        setupGroupValidation()
        setupAppLifecycleHandling()
        setupLocationFocusManagement()
        setupStateBuilding()
        setupHistorySubscription()
    }

    private fun setupHistorySubscription() {
        uiState.mapNotNull {
            it.asReady()
        }
            .map { it.mapHomeScreenFocusType.asLocationHistory }
            .distinctUntilChanged()
            .onEach { historyMode ->
                locationHistoryController.value?.onCleared()
                if (historyMode == null) {
                    locationHistoryController.update { null }
                } else {
                    locationHistoryController.update {
                        LocationHistoryViewModelController(
                            groupId = historyMode.groupId,
                            memberUserId = historyMode.memberUserId,
                            deviceId = historyMode.deviceId,
                            getLocationHistory = getLocationHistory,
                            refreshLocationHistory = refreshLocationHistory,
                            deleteLocationHistory = deleteLocationHistory,
                            userRepo = userRepository,
                            personalSpaceRepo = personalSpaceRepo,
                            userGroupRepo = groupRepo,
                            valueFormatter = valueFormatter,
                            appPrefs = appPrefs,
                            remoteConfig = remoteConfig,
                            geofenceManager = geofenceManager,
                            coroutineScope = coroutineScope,
                            messageDispatcher = messageDispatcher
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun setupGroupValidation() {
        allGroups.onEach { groupsData ->
            val selectedGroup = groupIdData.value.takeIf { it.id.isNotBlank() }
                ?: let {
                    if (appPrefs.isPersonalSpaceSelected) {
                        UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
                    } else {
                        appPrefs.selectedGroupId?.asGroupOrSpaceId()
                    }
                }
            val shouldChangeSelectedGroup = selectedGroup?.isGroup == true || selectedGroup == null
            if (groupsData is Resource.Ready && groupsData.data.isNotEmpty() && shouldChangeSelectedGroup) {
                if (groupsData.data.none { it.id == selectedGroup?.id }) {
                    mapEventHandler.onEvent(
                        MapHomeScreenUiEvent.SwitchViewSource(
                            viewSource = MapScreenViewSourceType.DefaultGroup(groupsData.data.firstOrNull()?.id?.asGroupOrSpaceId())
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
                        val id = groupIdData.value
                        if (id.isPersonalSpace) {
                            personalSpaceRefresh.refreshPersonalSpace(10.seconds.inWholeMilliseconds)
                        } else if (id.id.isNotBlank()) {
                            groupPreRefresh.refreshGroupWithMembers(id.id)
                        }
                    }
                } else {
                    logg.v(
                        "home_screen",
                        "App in foreground but no selected group - try to select default group"
                    )
                    mapEventHandler.onEvent(
                        MapHomeScreenUiEvent.SwitchViewSource(MapScreenViewSourceType.DefaultGroupOrSpace(null))
                    )
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
        ) { groupId, isAppFocused, isUiFocused, mapFocusType ->
            MapScreenLifecycleData(
                groupId = groupId,
                planConfig = uiState.value.asReady()?.groupPlanConfig ?: PlanConfig.default,
                isAppFocused = isAppFocused,
                isUiFocused = isUiFocused,
                mapFocusType = mapFocusType
            )
        }
            .debounce(2000)
            .distinctUntilChanged()
            .onEach {
                locationFocusManager.keepLocationFocusOn(lifecycleData = it)
            }
            .map {
                it.isAppFocused
            }
            .distinctUntilChanged()
            .onEach {
                locationFocusManager.initRealtime()
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
                        } else if (it.id.isNotBlank()) {
                            groupPreRefresh.refreshGroupWithMembers(it.id)
                        }
                    }
                }
            }
            .flatMapLatest {
                if (it.id.isBlank()) {
                    combine(
                        if (appPrefs.isPersonalSpaceSelected) {
                            flowOf(null)
                        } else {
                            groupsEmptyStateManager.getUiState()
                        },
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { groupsEmptyState, userData ->
                        val mapProvider = premiumStatusManager.defaultMapProvider()
                        MapHomeScreenState.Empty(
                            groupsEmptyState = groupsEmptyState,
                            selectedGroupId = null,
                            selectedGroupImage = null,
                            isPersonalSpace = appPrefs.isPersonalSpaceSelected,
                            currentUserImage = userData?.userImage ?: ImageModel.emoji(Emojis.robot),
                            androidMapSDKProvider = mapProvider,
                            isSatelliteMode = false
                        )
                    }
                } else if (it.isPersonalSpace){
                    loadPersonalSpaceContent()
                } else {
                    loadGroupContent(groupId = it.id)
                }
            }
            .onEach { _uiState.value = it }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun loadPersonalSpaceContent(): Flow<MapHomeScreenState> {
        return combine(
            uiRefreshPingFlow.filterNotNull(),
            personalSpaceRepo.personalSpaceState.map { it.data },
            placesRepo.getPlacesForGroupLocally(groupId = appPrefs.userId),
            locationFocusManager.getSharingContracts(),
            userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
        ) { _, space, places, sharingContracts, userData ->

            if (space == null || userData == null) {
                val mapProvider = premiumStatusManager.defaultMapProvider()
                return@combine MapHomeScreenState.Empty(
                    groupsEmptyState = null,
                    selectedGroupId = appPrefs.selectedGroupId,
                    selectedGroupImage = _uiState.value.selectedGroupImage,
                    isPersonalSpace = true,
                    currentUserImage = ImageModel.emoji(Emojis.robot),
                    androidMapSDKProvider = mapProvider,
                    isSatelliteMode = false
                )
            }

            mapStateMapper.buildReadyState(
                groupOrSpace = GroupWithMembersOrUserSpace.PersonalSpace(space),
                places = places,
                activeSharingContracts = sharingContracts,
                userData = userData,
                previousState = _uiState.value.asReady()
                    ?.takeIf { it.groupOrSpaceId.isPersonalSpace },
                isSatelliteMode = _uiState.value.isSatelliteMode,
                alertDialog = _uiState.value.alertDialog,
                isFirstTimeInPersonalSpace = _uiState.value.asReady()?.isFirstTimeInPersonalSpace ?: !appPrefs.didHavePersonalSpaceIntroOnboarding,
            )
        }
            .flowOn(Dispatchers.IO)
    }

    private fun loadGroupContent(groupId: String): Flow<MapHomeScreenState> {
        return combine(
            uiRefreshPingFlow.filterNotNull(),
            groupRepo.getGroupWithMembers(groupId = groupId),
            placesRepo.getPlacesForGroupLocally(groupId = groupId),
            locationFocusManager.getSharingContracts(),
            userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
        ) { _, group, places, sharingContracts, userData ->

            if (group == null || userData == null) {
                val mapProvider = premiumStatusManager.defaultMapProvider()
                return@combine MapHomeScreenState.Empty(
                    groupsEmptyState = null,
                    selectedGroupId = null,
                    selectedGroupImage = null,
                    isPersonalSpace = false,
                    currentUserImage = ImageModel.emoji(Emojis.robot),
                    androidMapSDKProvider = mapProvider,
                    isSatelliteMode = false
                )
            }

            mapStateMapper.buildReadyState(
                groupOrSpace = GroupWithMembersOrUserSpace.Group(group),
                places = places,
                activeSharingContracts = sharingContracts,
                userData = userData,
                previousState = _uiState.value.asReady()
                    ?.takeIf { it.groupOrSpaceId.id == group.id },
                isSatelliteMode = _uiState.value.isSatelliteMode,
                alertDialog = _uiState.value.alertDialog,
                isFirstTimeInPersonalSpace = _uiState.value.asReady()?.isFirstTimeInPersonalSpace ?: !appPrefs.didHavePersonalSpaceIntroOnboarding
            )
        }
            .flatMapLatest { state ->
                when (state) {
                    is MapHomeScreenState.Empty -> {
                        groupsEmptyStateManager.getUiState()
                            .map { state.copy(groupsEmptyState = it) }
                    }
                    is MapHomeScreenState.Ready -> flowOf(state)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    fun onEvent(event: MapHomeScreenUiEvent) {
        mapEventHandler.onEvent(event)
    }

    fun onEvent(event: LocationHistoryUiEvent) {
        if (event is LocationHistoryUiEvent.OnCloseScreen) {
            mapEventHandler.onEvent(MapHomeScreenUiEvent.CloseLocationHistory)
        } else {
            locationHistoryController.value?.onEvent(event)
        }
    }

    fun getGroupPickerState(showPersonalSpace: Boolean): BottomSheetPickerType.GroupPickerBottomSheet {
        return groupStateManager.getGroupPickerState(showPersonalSpace)
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
        locationHistoryController.value?.onCleared()
        locationHistoryController.update { null }
        groupSubscriptionJob.cancel()
        locationFocusManager.cancelJobs()
    }
}