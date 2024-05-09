package app.paralino.core.presentation.settings.appstatus

import app.paralino.core.provider.InAppLoggerLogType
import app.paralino.core.provider.LogPriority

data class AppStatusDebugScreenState(
    val query: String = "",
    val filters: AppStatusFilterState = AppStatusFilterState(),
    val status: List<AppStatusItemStatusState> = emptyList(),
    val logs: List<AppStatusItemLogState> = emptyList()
)

data class AppStatusItemStatusState(
    val key: String,
    val text: String,
    val isError: Boolean
)

data class AppStatusItemLogState(
    val key: String,
    val text: String,
    val priority: LogPriority
)

data class AppStatusFilterState(
    val logPriorityFilters: List<LogPriorityFilterItemState> = emptyList(),
    val logTypeFilters: List<LogTypeFilterItemState> = emptyList(),
)

data class LogPriorityFilterItemState(
    val logPriority: LogPriority,
    val isOn: Boolean
) {
    val key: String = logPriority.name
    val name: String = logPriority.name.lowercase()
}

data class LogTypeFilterItemState(
    val type: InAppLoggerLogType,
    val isOn: Boolean
) {
    val key: String = type.key
    val name: String = type.name.lowercase()
}

sealed class AppStatusDebugScreenEvent {

}

sealed class AppStatusDebugScreenUiEvent {
    data object ClearLogs : AppStatusDebugScreenUiEvent()
    data class SearchLogs(val query: String) : AppStatusDebugScreenUiEvent()
    data class ToggleLogPriorityFilter(val filter: LogPriorityFilterItemState) :
        AppStatusDebugScreenUiEvent()

    data class ToggleLogTypeFilter(val filter: LogTypeFilterItemState) :
        AppStatusDebugScreenUiEvent()
}