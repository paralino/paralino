package app.paralino.core.presentation.settings.appstatus

import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.InAppLogger
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
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

    private val searchEntry = MutableStateFlow("")

    init {
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
            inAppLogger.fullLog(),
            searchEntry.debounce(500.milliseconds),
        ) { logs, searchEntry ->
            _uiState.update {
                it.copy(
                    logs = logs
                        .filter {
                            it.tag.contains(searchEntry, ignoreCase = true) ||
                                    it.title.contains(searchEntry, ignoreCase = true) ||
                                    it.info?.contains(searchEntry, ignoreCase = true) ?: false ||
                                    it.type.contains(searchEntry, ignoreCase = true) ||
                                    LogPriority.fromKeyOrFloor(it.priority).name.contains(searchEntry, ignoreCase = true)
                        }
                        .map {
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
    }

    fun onEvent(event: AppStatusDebugScreenUiEvent) {
        when (event) {
            AppStatusDebugScreenUiEvent.ClearLogs -> {
                coroutineScope.launch { inAppLogger.clearLogs() }
            }

            is AppStatusDebugScreenUiEvent.SearchLogs -> {
                searchEntry.update { event.query }
                _uiState.update { it.copy(query = event.query) }
            }
        }
    }
}