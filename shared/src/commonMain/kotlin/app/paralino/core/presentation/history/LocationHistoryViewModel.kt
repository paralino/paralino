package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.history.LocationHistoryScreenEvent.OpenNavigation
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenExternalNavigation
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog.*
import app.paralino.core.utils.extensions.isoToLocalDateTime
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

    val dataController = LocationHistoryViewModelDataController(
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


    fun onEvent(event: LocationHistoryUiEvent) {
        when (event) {
            is LocationHistoryUiEvent.LoadHistory -> {
                dataController.loadHistory(groupId = event.groupId, memberUserId = event.memberUserId)
            }

            is LocationHistoryUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is DeleteLocationHistoryEntry -> {
                        dataController._uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == Buttons.buttonOk) {
                            dataController.deleteSelectedEntry()
                        } else if (event.action.action == DeleteLocationHistoryEntry.buttonMarkAsRangeId){
                            dataController.markForRangeDeletion()
                        }
                    }

                    is DeleteLocationHistoryRange -> {
                        dataController._uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == Buttons.buttonOk) {
                            dataController.deleteSelectedRange()
                        } else if (event.action.action == DeleteLocationHistoryRange.buttonClearSelection) {
                            dataController._uiState.update { it.copy(deletionRangeStartIndex = null) }
                        }
                    }

                    else -> {}
                }
            }

            LocationHistoryUiEvent.OnMapModeSwitch -> {
                dataController._uiState.update {
                    it.copy(
                        isSatelliteMode = !it.isSatelliteMode,
                        isFilterOpened = false
                    )
                }
            }

            LocationHistoryUiEvent.OnMapTouch -> {
                dataController._uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = LocationHistoryScreenFocusType.NONE
                    )
                }
            }

            LocationHistoryUiEvent.ToggleMapFilter -> {
                dataController._uiState.update { it.copy(isFilterOpened = !it.isFilterOpened) }
            }

            is LocationHistoryUiEvent.OnMarkerClick -> {
                dataController._uiState.update {
                    it.copy(
                        isFilterOpened = false,
                        focusType = if (it.focusType == LocationHistoryScreenFocusType.MEMBER_DEFAULT) {
                            LocationHistoryScreenFocusType.MEMBER_FOCUSED
                        } else {
                            LocationHistoryScreenFocusType.MEMBER_DEFAULT
                        }
                    )
                }
            }

            is LocationHistoryUiEvent.OnPointClicked -> {
                dataController._uiState.update {
                    it.copy(
                        selectedLocationId = event.point.pointId,
                        isFilterOpened = false
                    )
                }
            }

            is LocationHistoryUiEvent.OnSelectFocusType -> {
                dataController._uiState.update {
                    it.copy(
                        focusType = event.type,
                        isFilterOpened = false
                    )
                }
            }

            is LocationHistoryUiEvent.SetHistoryProgress -> {
                val newId = dataController._uiState.value.selectedLocations.getOrNull(
                    index = event.progress
                )?.pointId

                dataController._uiState.update {
                    it.copy(
                        selectedLocationId = newId,
                        isFilterOpened = false
                    )
                }
            }

            LocationHistoryUiEvent.ToggleDayRange -> {
                dataController.handleDaySelection(
                    dateRangeDays = when (uiState.value.dateRangeDays) {
                        1 -> 3
                        else -> 1
                    },
                    selectedDate = uiState.value.selectedDate,
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

            is LocationHistoryUiEvent.ChangeMapDataViewType -> {
                dataController._uiState.update {
                    it.copy(
                        dataViewType = event.type,
                        focusType = when (event.type) {
                            LocationHistoryDataViewType.LocationPoint -> LocationHistoryScreenFocusType.OVERVIEW
                            LocationHistoryDataViewType.Stats -> LocationHistoryScreenFocusType.OVERVIEW
                        },
                        isFilterOpened = false
                    )
                }
            }

            LocationHistoryUiEvent.OnDeleteSelectedLocationRequest -> {
                if (uiState.value.locationsToDelete.isNotEmpty()) {
                    val startTime = uiState.value.locationsToDelete.firstOrNull()?.let {
                        valueFormatter.formatTimeRangeForLocationHistory(
                            startTimeUTC = it.point.status.startTime.isoToLocalDateTime(),
                            endTimeUTC = null,
                            rangeStart = uiState.value.startDate,
                            rangeEnd = uiState.value.endDate
                        ).let {
                            "**$it**"
                        }
                    } ?: "-"
                    val endTime = uiState.value.locationsToDelete.lastOrNull()?.let {
                        valueFormatter.formatTimeRangeForLocationHistory(
                            startTimeUTC = it.point.status.endTime.isoToLocalDateTime(),
                            endTimeUTC = null,
                            rangeStart = uiState.value.startDate,
                            rangeEnd = uiState.value.endDate
                        ).let {
                            "**$it**"
                        }
                    } ?: "-"
                    dataController._uiState.update {
                        it.copy(
                            alertDialog = DeleteLocationHistoryRange(
                                startInfo = startTime,
                                endInfo = endTime,
                                count = uiState.value.locationsToDelete.size.toString().let { "**$it**" }
                            ),
                            isFilterOpened = false
                        )
                    }
                } else {
                    val time = dataController._uiState.value.selectedPoint?.locationTime?.let { "**$it**" } ?: "-"
                    dataController._uiState.update {
                        it.copy(
                            alertDialog = DeleteLocationHistoryEntry(
                                info = time,
                                canDeleteRange = uiState.value.selectedLocations.size > 1
                            ),
                            isFilterOpened = false
                        )
                    }
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

            LocationHistoryUiEvent.OpenLocationSettings -> {
                dataController._eventChannel.trySend(LocationHistoryScreenEvent.OpenHistorySettings)
            }

            LocationHistoryUiEvent.ToggleClusterLocations -> {
                dataController._uiState.update {
                    it.copy(
                        viewMode = when (it.viewMode) {
                            LocationHistoryPathViewMode.OVERVIEW -> LocationHistoryPathViewMode.DETAILED
                            LocationHistoryPathViewMode.DETAILED -> LocationHistoryPathViewMode.OVERVIEW
                        }
                    )
                }
            }

            LocationHistoryUiEvent.ToggleClusterPins -> {
                dataController._uiState.update {
                    it.copy(clusterPins = !it.clusterPins)
                }
            }

            LocationHistoryUiEvent.ToggleShowPins -> {
                dataController._uiState.update {
                    if (it.showPolyline) {
                        it.copy(showPins = !it.showPins)
                    } else {
                        it.copy(showPolyline = true, showPins = false)
                    }
                }
            }

            LocationHistoryUiEvent.ToggleShowPolyline -> {
                dataController._uiState.update {
                    if (it.showPins) {
                        it.copy(showPolyline = !it.showPolyline)
                    } else {
                        it.copy(showPolyline = false, showPins = true)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        dataController.clear()
        super.onCleared()
    }
}