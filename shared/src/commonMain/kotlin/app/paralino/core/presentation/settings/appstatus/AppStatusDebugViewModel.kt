package app.paralino.core.presentation.settings.appstatus

import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.InAppLogger
import app.paralino.core.provider.InAppLoggerLogType
import app.paralino.core.provider.LogPriority
import app.paralino.core.provider.print
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AppStatusDebugViewModel(
    private val inAppLogger: InAppLogger,
    private val deviceApi: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(AppStatusDebugScreenState())
    val uiState: Flow<AppStatusDebugScreenState> = _uiState

    private val searchEntryFlow = MutableStateFlow("")
    private val filtersFlow = MutableStateFlow(AppStatusFilterState())

    init {

        filtersFlow.update {
            it.copy(
                logTypeFilters = InAppLoggerLogType.entries.map {
                    LogTypeFilterItemState(
                        type = it,
                        isOn = false
                    )
                },
                logPriorityFilters = LogPriority.entries.map {
                    LogPriorityFilterItemState(
                        logPriority = it,
                        isOn = false
                    )
                }
            )
        }

        combine(
            deviceApi.deviceStatus,
            locationSharingManager.allContractsFlow,
            locationPerformanceManager.locationPerformanceType
        ) { deviceStatus, sharingStatus, locationPerformance ->
            _uiState.update {
                it.copy(
                    status = buildList {
                        add(
                            AppStatusItemStatusState(
                                key = "location-sharing",
                                text = "Sharing location to (${sharingStatus.data?.size ?: 0}) groups",
                                isError = false
                            )
                        )
                        add(
                            AppStatusItemStatusState(
                                key = "network",
                                text = "Network: **${deviceStatus.networkStatus.status} (${deviceStatus.networkStatus.networkType})**",
                                isError = !deviceStatus.networkStatus.isOnline
                            )
                        )
                        add(
                            AppStatusItemStatusState(
                                key = "location-performance",
                                text = "Location performance: **${locationPerformance.current}**",
                                isError = false
                            )
                        )

                        add(
                            AppStatusItemStatusState(
                                key = "notification-permission",
                                text = "Notification permission: ${if (deviceStatus.locationStatus.isNotificationPermissionAllowed) "**OK**" else "**MISSING**"}",
                                isError = !deviceStatus.locationStatus.isNotificationPermissionAllowed
                            )
                        )

                        add(
                            AppStatusItemStatusState(
                                key = "device-location",
                                text = "Device location: ${if (deviceStatus.locationStatus.isGpsOn) "**ON**" else "**OFF**"}",
                                isError = !deviceStatus.locationStatus.isGpsOn
                            )
                        )

                        add(
                            AppStatusItemStatusState(
                                key = "location-permission",
                                text = "Location permission: ${if (deviceStatus.locationStatus.isPermissionAllowed) "**OK**" else "**MISSING**"}",
                                isError = !deviceStatus.locationStatus.isPermissionAllowed
                            )
                        )

                        add(
                            AppStatusItemStatusState(
                                key = "location-permission-background",
                                text = "Background location permission: ${if (deviceStatus.locationStatus.isBackgroundPermissionAllowed) "**OK**" else "**MISSING**"}",
                                isError = !deviceStatus.locationStatus.isBackgroundPermissionAllowed
                            )
                        )
                    }
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            searchEntryFlow.debounce(500.milliseconds),
            filtersFlow
        ) { searchEntry, filters ->
            searchEntry to filters
        }.flatMapLatest {
            inAppLogger.getFilteredLog(
                priorities = it.second.logPriorityFilters
                    .filter { it.isOn }
                    .map { it.logPriority },
                types = it.second.logTypeFilters
                    .filter { it.isOn }
                    .map { it.type },
                tags = emptyList(),
                text = it.first
            )
        }
            .onEach { logs ->
                _uiState.update {
                    it.copy(
                        logs = logs.map {
                            AppStatusItemLogState(
                                key = it.id.toString(),
                                text = it.print(),
                                priority = LogPriority.fromKeyOrFloor(it.priority)
                            )
                        }
                    )
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            searchEntryFlow,
            filtersFlow
        ) { searchQuery, filters ->
            _uiState.update {
                it.copy(query = searchQuery, filters = filters)
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: AppStatusDebugScreenUiEvent) {
        when (event) {
            AppStatusDebugScreenUiEvent.ClearLogs -> {
                coroutineScope.launch { inAppLogger.clearLogs() }
            }

            is AppStatusDebugScreenUiEvent.SearchLogs -> {
                searchEntryFlow.update { event.query }
            }

            is AppStatusDebugScreenUiEvent.ToggleLogPriorityFilter -> {
                filtersFlow.update {
                    it.copy(
                        logPriorityFilters = it.logPriorityFilters.map {
                            if (it.key == event.filter.key) {
                                it.copy(isOn = !it.isOn)
                            } else {
                                it
                            }
                        }
                    )
                }
            }

            is AppStatusDebugScreenUiEvent.ToggleLogTypeFilter -> {
                filtersFlow.update {
                    it.copy(
                        logTypeFilters = it.logTypeFilters.map {
                            if (it.key == event.filter.key) {
                                it.copy(isOn = !it.isOn)
                            } else {
                                it
                            }
                        }
                    )
                }
            }
        }
    }
}