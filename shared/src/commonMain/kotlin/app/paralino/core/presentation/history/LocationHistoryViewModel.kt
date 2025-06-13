package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.GetLocationHistoryUseCase
import app.paralino.core.domain.location.RefreshLocationHistoryUseCase
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
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
    private val remoteConfig: RemoteConfigRepository
) : CoreViewModel() {

    private val dataController = LocationHistoryViewModelDataController(
        getLocationHistory = getLocationHistory,
        refreshLocationHistory = refreshLocationHistory,
        userRepo = userRepo,
        userGroupRepo = userGroupRepo,
        valueFormatter = valueFormatter,
        appPrefs = appPrefs,
        remoteConfig = remoteConfig,
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
            is LocationHistoryUiEvent.OnMarkerClick -> TODO()
            LocationHistoryUiEvent.OnRefocusClick -> TODO()
            is LocationHistoryUiEvent.SelectLocation -> {}
            is LocationHistoryUiEvent.SetHistoryProgress -> TODO()
            LocationHistoryUiEvent.OnNextDay -> TODO()
            LocationHistoryUiEvent.OnPreviousDay -> TODO()
            LocationHistoryUiEvent.ToggleDayRange -> {

            }

            is LocationHistoryUiEvent.OnMapLongPress -> {}
            is LocationHistoryUiEvent.SelectDevice -> {
                dataController._uiState.update {
                    it.copy(
                        devices = it.devices.map {
                            when (it) {
                                is UserActiveLocationDeviceState.Known -> it.copy(isSelected = it.locationId == event.device.locationId)
                                is UserActiveLocationDeviceState.Unknown -> it.copy(isSelected = it.locationId == event.device.locationId)
                            }
                        }
                    )
                }
            }
            is LocationHistoryUiEvent.UiLifecycleChange -> {
                uiFocusedStatus.update { event.active }
            }
        }
    }

    override fun onCleared() {
        historySubscriptionJob?.cancel()
        super.onCleared()
    }
}