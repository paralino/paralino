package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.GetLocationHistoryUseCase
import app.paralino.core.domain.location.RefreshLocationHistoryUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class LocationHistoryViewModel(
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val userRepo: UserRepository,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val geofenceManager: IGeofenceManager
) : CoreViewModel() {

    private val dataController = LocationHistoryViewModelDataController(
        getLocationHistory = getLocationHistory,
        refreshLocationHistory = refreshLocationHistory,
        userRepo = userRepo,
        userGroupRepo = userGroupRepo,
        valueFormatter = valueFormatter,
        appPrefs = appPrefs,
        remoteConfig = remoteConfig,
        geofenceManager = geofenceManager,
        viewModel = this
    )

    val uiState: StateFlow<LocationHistoryScreenState> = dataController._uiState
    val events = dataController._eventChannel.receiveAsFlow()

    var historySubscriptionJob: Job? = null
    private val uiFocusedStatus = MutableStateFlow(false)


    init {

    }

    fun loadHistory(
        groupId: String,
        memberUserId: String
    ) {
        historySubscriptionJob?.cancel()
        historySubscriptionJob = dataController.dataFetchController.loadHistory(
            groupId = groupId,
            memberUserId = memberUserId
        )
            .launchIn(coroutineScope)
    }

    fun onEvent(event: LocationHistoryUiEvent) {
        when (event) {
            is LocationHistoryUiEvent.DialogAction -> TODO()
            LocationHistoryUiEvent.OnCloseFocusedItemClick -> TODO()
            LocationHistoryUiEvent.OnMapModeSwitch -> {
                dataController._uiState.update { it.copy(isSatelliteMode = !it.isSatelliteMode) }
            }
            LocationHistoryUiEvent.OnMapTouch -> {}
            is LocationHistoryUiEvent.OnMarkerClick -> {}
            LocationHistoryUiEvent.OnRefocusClick -> TODO()
            is LocationHistoryUiEvent.SelectLocation -> {}
            is LocationHistoryUiEvent.SetHistoryProgress -> {
                dataController._uiState.update {
                    it.copy(selectedLocationIndex = event.progress)
                }
            }
            LocationHistoryUiEvent.OnNextDay -> TODO()
            LocationHistoryUiEvent.OnPreviousDay -> TODO()
            LocationHistoryUiEvent.ToggleDayRange -> {

            }

            is LocationHistoryUiEvent.OnMapLongPress -> {}
            is LocationHistoryUiEvent.SelectDevice -> {
                dataController._uiState.update {
                    val newState = it.copy(
                        devices = it.devices.map {
                            when (it) {
                                is UserActiveLocationDeviceState.Known -> it.copy(isSelected = it.locationId == event.device.locationId)
                                is UserActiveLocationDeviceState.Unknown -> it.copy(isSelected = it.locationId == event.device.locationId)
                            }
                        },
                        selectedLocationIndex = null
                    )

                    val stats = LocationHistoryStats.calculateStats(
                        state = newState,
                        valueFormatter = dataController.valueFormatter,
                        geofenceManager = dataController.geofenceManager
                    )

                    newState.copy(stats = stats)
                }
            }

            is LocationHistoryUiEvent.UiLifecycleChange -> {
                uiFocusedStatus.update { event.active }
            }

            LocationHistoryUiEvent.OnCloseScreen -> {
                dataController._eventChannel.trySend(LocationHistoryScreenEvent.NavigateBack)
            }

            is LocationHistoryUiEvent.ChangeMapFocusType -> {
                dataController._uiState.update {
                    it.copy(mapFocusType = event.type)
                }
            }
        }
    }

    override fun onCleared() {
        historySubscriptionJob?.cancel()
        super.onCleared()
    }
}