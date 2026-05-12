package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.history.LocationHistoryScreenEvent.OpenNavigation
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenExternalNavigation
import app.paralino.core.models.settings.MapInteractionsType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.AppSnackbarPayload
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.MainAlertDialog.Buttons
import app.paralino.core.presentation.common.MainAlertDialog.DeleteLocationHistoryEntry
import app.paralino.core.presentation.common.MainAlertDialog.DeleteLocationHistoryRange
import app.paralino.core.presentation.common.onAction
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

            LocationHistoryUiEvent.OnMapTouch -> {
                _uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = LocationHistoryScreenFocusType.NONE_COLLAPSED(
                            isForceCollapsedRequested = it.focusType.isForceCollapsedRequested
                        ),
                        previousFocusType = if (it.focusType.isCollapsed) {
                            it.previousFocusType
                        } else {
                            it.focusType
                        }
                    )
                }
            }

            is LocationHistoryUiEvent.OnMapClick -> {
                // do nothing
            }

            LocationHistoryUiEvent.ToggleMapFilter -> {
                _uiState.update { it.copy(isFilterOpened = !it.isFilterOpened) }
            }

            is LocationHistoryUiEvent.OnMarkerClick -> {
                _uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = LocationHistoryScreenFocusType.MEMBER(
                            isFocused = it.focusType.isMemberNotFocused,
                            isCollapsed = false,
                            isForceCollapsedRequested = false
                        ),
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
                        focusType = if (it.focusType is LocationHistoryScreenFocusType.NONE_COLLAPSED) {
                            LocationHistoryScreenFocusType.MEMBER(
                                isFocused = false,
                                isCollapsed = true,
                                isForceCollapsedRequested = it.focusType.isForceCollapsedRequested
                            )
                        } else {
                            it.focusType
                        },
//                        focusType = it.focusType,
                        previousFocusType = it.previousFocusType,
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

            LocationHistoryUiEvent.ToggleExpandedCollapseFocusType -> {
                when (uiState.value.dataViewType) {
                    LocationHistoryDataViewType.LocationPoint -> {
                        when (val focus = uiState.value.focusType) {
                            is LocationHistoryScreenFocusType.MEMBER -> {
                                onEvent(
                                    LocationHistoryUiEvent.OnSelectFocusType(
                                        LocationHistoryScreenFocusType.MEMBER(
                                            isCollapsed = !focus.isCollapsed,
                                            isFocused = focus.isFocused,
                                            isForceCollapsedRequested = !focus.isCollapsed
                                        )
                                    )
                                )
                            }

                            is LocationHistoryScreenFocusType.NONE_COLLAPSED,
                            is LocationHistoryScreenFocusType.OVERVIEW -> {
                                onEvent(
                                    LocationHistoryUiEvent.OnSelectFocusType(
                                        LocationHistoryScreenFocusType.MEMBER(
                                            isCollapsed = !uiState.value.isCardCollapsed,
                                            isFocused = false,
                                            isForceCollapsedRequested = !uiState.value.isCardCollapsed
                                        )
                                    )
                                )
                            }
                        }
                    }

                    LocationHistoryDataViewType.Stats -> {
                        onEvent(
                            LocationHistoryUiEvent.OnSelectFocusType(
                                LocationHistoryScreenFocusType.OVERVIEW(
                                    isCollapsed = !uiState.value.focusType.isCollapsed,
                                    isForceCollapsedRequested = !uiState.value.focusType.isCollapsed
                                )
                            )
                        )
                    }
                }
            }

            LocationHistoryUiEvent.OnFocusOnMemberClick -> {
                when (val focus = uiState.value.focusType) {
                    is LocationHistoryScreenFocusType.MEMBER -> {
                        onEvent(
                            LocationHistoryUiEvent.OnSelectFocusType(
                                LocationHistoryScreenFocusType.MEMBER(
                                    isCollapsed = false,
                                    isFocused = true,
                                    isForceCollapsedRequested = false
                                )
                            )
                        )
                    }

                    else -> {
                        onEvent(
                            LocationHistoryUiEvent.OnSelectFocusType(
                                LocationHistoryScreenFocusType.MEMBER(
                                    isCollapsed = focus.isCollapsed,
                                    isFocused = false,
                                    isForceCollapsedRequested = focus.isForceCollapsedRequested
                                )
                            )
                        )
                    }
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
                        focusType = LocationHistoryScreenFocusType.OVERVIEW(
                            isCollapsed = false,
                            isForceCollapsedRequested = false
                        ),
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
                                            val days =
                                                _uiState.value.planConfig?.personalHistoryDaysLimit
                                                    ?: 0
                                            if (days == 0) {
                                                MR.strings.history_map_plan_limit_message_personal_free.localizedString()
                                            } else {
                                                MR.strings.history_map_plan_limit_message_personal_count.localizedString(
                                                    _uiState.value.planConfig?.personalHistoryDaysLimit
                                                        ?: 1
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

            LocationHistoryUiEvent.ToggleShowInaccurateLocation -> {
                _uiState.update {
                    it.copy(showInaccurateLocations = !it.showInaccurateLocations)
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

            is LocationHistoryUiEvent.OnScreenDensityChange -> {
                _uiState.update { state ->
                    state.copy(
                        mapInteractionType = MapInteractionsType.forScreenSize(
                            defaultType = appPrefs.mapInteractions,
                            maxWidthDp = event.maxWidthDp,
                            maxHeightDp = event.maxHeightDp
                        )
                    )
                }
            }
        }
    }

    fun onCleared() {
        historyEventHandler.clear()
    }
}