package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.history.LocationHistoryScreenEvent.OpenNavigation
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenExternalNavigation
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.AppSnackbarPayload
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog.Buttons
import app.paralino.core.ui.components.dialogs.MainAlertDialog.DeleteLocationHistoryEntry
import app.paralino.core.ui.components.dialogs.MainAlertDialog.DeleteLocationHistoryRange
import app.paralino.core.ui.components.onAction
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationHistoryViewModel(
    groupId: String,
    memberUserId: String,
    deviceId: String?,
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
    private val userRepo: UserRepository,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val geofenceManager: IGeofenceManager
) : CoreViewModel() {

    val controller = LocationHistoryViewModelController(
        groupId = groupId,
        memberUserId = memberUserId,
        deviceId = deviceId,
        getLocationHistory = getLocationHistory,
        refreshLocationHistory = refreshLocationHistory,
        deleteLocationHistory = deleteLocationHistory,
        userRepo = userRepo,
        personalSpaceRepo = personalSpaceRepo,
        userGroupRepo = userGroupRepo,
        valueFormatter = valueFormatter,
        appPrefs = appPrefs,
         remoteConfig = remoteConfig,
        geofenceManager = geofenceManager,
        coroutineScope = coroutineScope,
        messageDispatcher = messageDispatcher
    )

    val uiState = controller.uiState
    val events = controller.events

    fun onEvent(event: LocationHistoryUiEvent) {
        controller.onEvent(event)
    }

    override fun onCleared() {
        controller.onCleared()
        super.onCleared()
    }
}

class LocationHistoryViewModelController(
    groupId: String,
    memberUserId: String,
    deviceId: String?,
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
    private val userRepo: UserRepository,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val geofenceManager: IGeofenceManager,
    private val coroutineScope: CoroutineScope,
    private val messageDispatcher: ICoreMessageDispatcher
) {

    private val _uiState = MutableStateFlow<LocationHistoryScreenState>(
        LocationHistoryScreenState.emptyState()
    )

    private val _eventChannel = Channel<LocationHistoryScreenEvent?>()

    private val clusteringService = LocationClusteringService(valueFormatter)
    private val defaultMapper = ILocationHistoryStateMapper(
        valueFormatter = valueFormatter,
        geofenceManager = geofenceManager,
        clusteringService = clusteringService,
        _uiState = _uiState,
    )

    private val personalStateMapper = LocationHistoryPersonalStateMapper(
        defaultMapper = defaultMapper,
        _uiState = _uiState,
    )

    private val groupStateMapper = LocationHistoryGroupStateMapper(
        defaultMapper = defaultMapper,
        remoteConfig = remoteConfig,
        _uiState = _uiState,
    )

    private val historyDateMapper = LocationHistoryDateMapper(
        userRepo = userRepo,
        userGroupRepo = userGroupRepo,
        remoteConfig = remoteConfig,
        valueFormatter = valueFormatter
    )

    private val dataFetchController = LocationHistoryDataFetchController(
        personalSpaceRepo = personalSpaceRepo,
        userGroupRepo = userGroupRepo,
        getLocationHistory = getLocationHistory,
        refreshLocationHistory = refreshLocationHistory,
        appPrefs = appPrefs,
        remoteConfig = remoteConfig,
        historyDateMapper = historyDateMapper,
        personalStateMapper = personalStateMapper,
        groupStateMapper = groupStateMapper,
        _uiState = _uiState,
        coroutineScope = coroutineScope,
        messageDispatcher = messageDispatcher
    )

    private val historyEventHandler = LocationHistoryEventHandler(
        deleteLocationHistory = deleteLocationHistory,
        valueFormatter = valueFormatter,
        geofenceManager = geofenceManager,
        dataFetchController = dataFetchController,
        messageDispatcher = messageDispatcher,
        coroutineScope = coroutineScope,
        historyDateMapper = historyDateMapper,
        _uiState = _uiState,
    )

    val uiState: StateFlow<LocationHistoryScreenUiState> = _uiState.map {
        LocationHistoryScreenUiState.fromScreenState(it)
    }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = LocationHistoryScreenUiState.fromScreenState(_uiState.value)
        )

    val events = _eventChannel.receiveAsFlow()

    init {
        historyEventHandler.loadHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            preselectedDeviceId = deviceId
        )
    }

    fun onEvent(event: LocationHistoryUiEvent) {
        when (event) {
            is LocationHistoryUiEvent.ReloadHistory -> {
                historyEventHandler.loadHistory(
                    groupId = event.groupId,
                    memberUserId = event.memberUserId,
                    preselectedDeviceId = event.preselectedDeviceId
                )
            }

            is LocationHistoryUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is DeleteLocationHistoryEntry -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == Buttons.buttonOk) {
                            historyEventHandler.deleteSelectedEntry()
                        } else if (event.action.action == DeleteLocationHistoryEntry.buttonMarkAsRangeId) {
                            historyEventHandler.markForRangeDeletion()
                        }
                    }

                    is DeleteLocationHistoryRange -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == Buttons.buttonOk) {
                            historyEventHandler.deleteSelectedRange()
                        } else if (event.action.action == DeleteLocationHistoryRange.buttonClearSelection) {
                            _uiState.update { it.copy(deletionRangeStartIndex = null) }
                        }
                    }

                    else -> {}
                }
            }

            LocationHistoryUiEvent.OnMapModeSwitch -> {
                _uiState.update {
                    it.copy(
                        isSatelliteMode = !it.isSatelliteMode,
                        isFilterOpened = false
                    )
                }
            }

            LocationHistoryUiEvent.OnMapTouch -> {
                _uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = LocationHistoryScreenFocusType.NONE,
                        previousFocusType = if (it.focusType == LocationHistoryScreenFocusType.NONE) {
                            it.previousFocusType
                        } else {
                            it.focusType
                        }
                    )
                }
            }

            LocationHistoryUiEvent.ToggleMapFilter -> {
                _uiState.update { it.copy(isFilterOpened = !it.isFilterOpened) }
            }

            is LocationHistoryUiEvent.OnMarkerClick -> {
                _uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = if (it.focusType == LocationHistoryScreenFocusType.MEMBER_DEFAULT) {
                            LocationHistoryScreenFocusType.MEMBER_FOCUSED
                        } else {
                            LocationHistoryScreenFocusType.MEMBER_DEFAULT
                        },
                        previousFocusType = it.focusType
                    )
                }
            }

            is LocationHistoryUiEvent.OnPointIdClicked -> {
                _uiState.value.selectedLocations.firstOrNull {
                    it.pointId == event.pointId
                }?.let {
                    onEvent(LocationHistoryUiEvent.OnPointClicked(point = it))
                }
            }

            is LocationHistoryUiEvent.OnPointClicked -> {
                _uiState.update {
                    it.copy(
                        dataViewType = LocationHistoryDataViewType.LocationPoint,
                        focusType = if (it.focusType == LocationHistoryScreenFocusType.NONE) {
                            LocationHistoryScreenFocusType.MEMBER_DEFAULT
                        } else {
                            it.focusType
                        },
                        previousFocusType = it.focusType,
                        selectedLocationId = event.point.pointId,
                        isFilterOpened = false
                    )
                }
            }

            is LocationHistoryUiEvent.OnSelectFocusType -> {
                _uiState.update {
                    it.copy(
                        focusType = event.type,
                        previousFocusType = it.focusType,
                        isFilterOpened = false
                    )
                }
            }

            is LocationHistoryUiEvent.SetHistoryProgress -> {
                val newId = _uiState.value.selectedLocations.getOrNull(
                    index = event.progress
                )?.pointId

                _uiState.update {
                    it.copy(
                        selectedLocationId = newId,
                        isFilterOpened = false
                    )
                }
            }

            is LocationHistoryUiEvent.NavigateHistoryPrevious -> {
                val newId = _uiState.value.selectedLocations.getOrNull(
                    index = _uiState.value.locationIndex - 1
                )?.pointId

                if (!newId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            selectedLocationId = newId,
                            isFilterOpened = false
                        )
                    }
                }
            }

            is LocationHistoryUiEvent.NavigateHistoryNext -> {
                val newId = _uiState.value.selectedLocations.getOrNull(
                    index = _uiState.value.locationIndex + 1
                )?.pointId

                if (!newId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            selectedLocationId = newId,
                            isFilterOpened = false
                        )
                    }
                }
            }

            LocationHistoryUiEvent.ToggleDayRange -> {
                historyEventHandler.handleDaySelection(
                    dateRangeDays = 1,
                    selectedDate = _uiState.value.selectedDate,
                )
            }

            is LocationHistoryUiEvent.OnMapLongPress -> {}

            is LocationHistoryUiEvent.SelectDevice -> {
                historyEventHandler.selectDevice(event.device)
            }

            LocationHistoryUiEvent.OnCloseScreen -> {
                _eventChannel.trySend(LocationHistoryScreenEvent.NavigateBack)
            }

            is LocationHistoryUiEvent.ChangeMapDataViewType -> {
                _uiState.update {
                    it.copy(
                        dataViewType = event.type,
                        focusType = when (event.type) {
                            LocationHistoryDataViewType.LocationPoint -> LocationHistoryScreenFocusType.OVERVIEW
                            LocationHistoryDataViewType.Stats -> LocationHistoryScreenFocusType.OVERVIEW
                        },
                        previousFocusType = it.focusType,
                        isFilterOpened = false
                    )
                }
            }

            LocationHistoryUiEvent.OnDeleteSelectedLocationRequest -> {
                if (_uiState.value.locationsToDelete.isNotEmpty()) {
                    val startTime = _uiState.value.locationsToDelete.firstOrNull()?.let {
                        valueFormatter.formatTimeRangeForLocationHistory(
                            startTimeUTC = it.point.status.startTime.isoToLocalDateTime(),
                            endTimeUTC = null,
                            rangeStart = _uiState.value.startDate,
                            rangeEnd = _uiState.value.endDate
                        ).let {
                            "**$it**"
                        }
                    } ?: "-"
                    val endTime = _uiState.value.locationsToDelete.lastOrNull()?.let {
                        valueFormatter.formatTimeRangeForLocationHistory(
                            startTimeUTC = it.point.status.endTime.isoToLocalDateTime(),
                            endTimeUTC = null,
                            rangeStart = _uiState.value.startDate,
                            rangeEnd = _uiState.value.endDate
                        ).let {
                            "**$it**"
                        }
                    } ?: "-"
                    _uiState.update {
                        it.copy(
                            alertDialog = DeleteLocationHistoryRange(
                                startInfo = startTime,
                                endInfo = endTime,
                                count = _uiState.value.locationsToDelete.size.toString()
                                    .let { "**$it**" }
                            ),
                            isFilterOpened = false
                        )
                    }
                } else {
                    val time =
                        _uiState.value.selectedPoint?.locationTime?.let { "**$it**" } ?: "-"
                    _uiState.update {
                        it.copy(
                            alertDialog = DeleteLocationHistoryEntry(
                                info = time,
                                canDeleteRange = _uiState.value.selectedLocations.size > 1
                            ),
                            isFilterOpened = false
                        )
                    }
                }
            }

            is LocationHistoryUiEvent.OnDirectionsRequest -> {
                _uiState.value.navigateAction?.let {
                    _eventChannel.trySend(
                        OpenNavigation(
                            OpenExternalNavigation(
                                lat = it.lat,
                                lng = it.lng,
                                name = it.title,
                                navigationApp = appPrefs.iosNavigationApp
                            )
                        )
                    )
                }
            }

            is LocationHistoryUiEvent.OnDateSelectorAction -> {
                when (event.action) {
                    LocationHistoryDateSelectionAction.LOAD_MODE -> {
                        historyEventHandler.addPreviousMonth()
                    }

                    LocationHistoryDateSelectionAction.UPGRADE -> {
                        coroutineScope.launch {
                            messageDispatcher.showSnackbarForResult(
                                data = AppSnackbarData(
                                    payload = AppSnackbarPayload(
                                        text = if (_uiState.value.isPersonalHistory) {
                                            val days = _uiState.value.planConfig?.personalHistoryDaysLimit ?: 0
                                            if (days == 0) {
                                                MR.strings.history_map_plan_limit_message_personal_free.localizedString()
                                            } else {
                                                MR.strings.history_map_plan_limit_message_personal_count.localizedString(
                                                    _uiState.value.planConfig?.personalHistoryDaysLimit ?: 1
                                                )
                                            }
                                        } else {
                                            MR.strings.history_map_plan_limit_message_owner.localizedString()
                                        },
                                        title = null,
                                        hasCloseButton = true
                                    ),
                                    actionText = MR.strings.button_continue.localizedString(),
                                    duration = SnackbarDuration.Forever
                                )
                            )?.result?.onAction {
                                _eventChannel.trySend(LocationHistoryScreenEvent.OpenPaywall)
                            }
                        }
                    }

                    LocationHistoryDateSelectionAction.ASK_FOR_UPGRADE -> {
                        coroutineScope.launch {
                            // ignore result, this will just show a button that will dismiss snackbar
                            messageDispatcher.showSnackbarForResult(
                                data = AppSnackbarData(
                                    payload = AppSnackbarPayload(
                                        text = MR.strings.history_map_plan_limit_message_member.localizedString(),
                                        hasCloseButton = true
                                    ),
                                    actionText = MR.strings.button_ok.localizedString(),
                                    duration = SnackbarDuration.Forever
                                )
                            )
                        }
                    }

                    LocationHistoryDateSelectionAction.END,
                    LocationHistoryDateSelectionAction.NONE -> {
                    }
                }
            }

            is LocationHistoryUiEvent.OnSelectDay -> {
                historyEventHandler.handleDaySelection(
                    dateRangeDays = _uiState.value.daySelectorState.dateRangeDays ?: 1,
                    selectedDate = event.day
                )
            }

            is LocationHistoryUiEvent.OnLongSelectDay -> {
                historyEventHandler.handleDayLongCLick(
                    selectedDate = event.day
                )
            }

            LocationHistoryUiEvent.OpenLocationSettings -> {
                _eventChannel.trySend(LocationHistoryScreenEvent.OpenHistorySettings)
            }

            LocationHistoryUiEvent.ToggleClusterLocations -> {
                _uiState.update {
                    it.copy(
                        clusteringPointsMode = when (it.clusteringPointsMode) {
                            LocationHistoryClusteringPointsViewMode.DEFAULT_CLUSTERING,
                            LocationHistoryClusteringPointsViewMode.CLUSTERING -> LocationHistoryClusteringPointsViewMode.DETAILED
                            LocationHistoryClusteringPointsViewMode.DEFAULT_DETAILED,
                            LocationHistoryClusteringPointsViewMode.DETAILED -> LocationHistoryClusteringPointsViewMode.CLUSTERING
                        }
                    )
                }
            }

            LocationHistoryUiEvent.ToggleClusterPins -> {
                _uiState.update {
                    it.copy(
                        clusteringPinsMode = when (it.clusteringPinsMode) {
                            LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING,
                            LocationHistoryClusteringPinsViewMode.CLUSTERING -> LocationHistoryClusteringPinsViewMode.DETAILED
                            LocationHistoryClusteringPinsViewMode.DEFAULT_DETAILED,
                            LocationHistoryClusteringPinsViewMode.DETAILED -> LocationHistoryClusteringPinsViewMode.CLUSTERING
                        }
                    )
                }
            }

            LocationHistoryUiEvent.ToggleShowPins -> {
                _uiState.update {
                    if (it.showPolyline) {
                        it.copy(showPins = !it.showPins)
                    } else {
                        it.copy(showPolyline = true, showPins = false)
                    }
                }
            }

            LocationHistoryUiEvent.ToggleShowPolyline -> {
                _uiState.update {
                    if (it.showPins) {
                        it.copy(showPolyline = !it.showPolyline)
                    } else {
                        it.copy(showPolyline = false, showPins = true)
                    }
                }
            }

            is LocationHistoryUiEvent.ExpandCluster -> {
                _uiState.update {
                    it.copy(
                        locations = it.locations.map {
                            when (it) {
                                is LocationHistoryPointState.Cluster -> {
                                    if (it.pointId == event.cluster.pointId) {
                                        it.copy(isExpanded = true)
                                    } else {
                                        it
                                    }
                                }
                                is LocationHistoryPointState.Point -> it
                            }
                        }
                    )
                }
            }

            is LocationHistoryUiEvent.OnRawDataPointClicked -> {
                _eventChannel.trySend(
                    LocationHistoryScreenEvent.OpenRawDataScreen(
                        type = event.action
                    )
                )
            }
        }
    }

    fun onCleared() {
        historyEventHandler.clear()
    }
}