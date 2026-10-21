package app.paralino.core.presentation.settings.appstatus

import app.paralino.core.models.AppHost
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.InAppLogger
import app.paralino.core.provider.InAppLoggerLogType
import app.paralino.core.provider.LogPriority
import app.paralino.core.provider.print
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
    private val valueFormatter: IValueFormatter
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
                },
                logHostsFilters = AppHost.entries.map {
                    LogAppHostFilterItemState(
                        host = it,
                        isOn = false
                    )
                }
            )
        }

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
                hosts = it.second.logHostsFilters
                    .filter { it.isOn }
                    .map { it.host },
                text = it.first
            )
        }
            .onEach { logs ->
                _uiState.update {
                    it.copy(
                        logs = logs.map {
                            AppStatusItemLogState(
                                key = it.id.toString(),
                                text = it.print(valueFormatter),
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

            is AppStatusDebugScreenUiEvent.ToggleAppHostTypeFilter -> {
                filtersFlow.update {
                    it.copy(
                        logHostsFilters = it.logHostsFilters.map {
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