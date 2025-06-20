package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.history.LocationHistoryScreenEvent.OpenNavigation
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenExternalNavigation
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class LocationHistoryViewModel(
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
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
        deleteLocationHistory = deleteLocationHistory,
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

    private val uiFocusedStatus = MutableStateFlow(false)


    init {

    }

    fun onEvent(event: LocationHistoryUiEvent) {
        when (event) {
            is LocationHistoryUiEvent.LoadHistory -> {
                dataController.loadHistory(groupId = event.groupId, memberUserId = event.memberUserId)
            }
            is LocationHistoryUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.DeleteLocationHistoryEntry -> {
                        dataController._uiState.update { it.copy(alertDialog = null) }
                        dataController.deleteSelectedEntry()
                    }

                    else -> {}
                }
            }
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

            LocationHistoryUiEvent.ToggleDayRange -> {
                dataController.handleDaySelection(
                    dateRangeDays = when (uiState.value.dateRangeDays) {
                        1 -> 3
                        else -> 1
                    },
                    selectedDate = uiState.value.selectedDate
                )
            }

            is LocationHistoryUiEvent.OnMapLongPress -> {}
            is LocationHistoryUiEvent.SelectDevice -> {
                dataController.selectDevice(event.device)
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

            LocationHistoryUiEvent.OnDeleteSelectedLocationRequest -> {
                val time = dataController._uiState.value.selectedPoint?.locationTime?.let { "**$it**" } ?: "-"
                dataController._uiState.update {
                    it.copy(alertDialog = MainAlertDialog.DeleteLocationHistoryEntry(info = time))
                }
            }

            is LocationHistoryUiEvent.OnDirectionsRequest -> {
                dataController._uiState.value.navigateAction?.let {
                    dataController._eventChannel.trySend(
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

            LocationHistoryUiEvent.OnPreviousMonth -> {
                dataController.addPreviousMonth()
            }

            is LocationHistoryUiEvent.OnSelectDay -> {
                dataController.handleDaySelection(
                    dateRangeDays = uiState.value.dateRangeDays,
                    selectedDate = event.day
                )
            }
        }
    }

    override fun onCleared() {
        dataController.clear()
        super.onCleared()
    }
}