package app.paralino.core.presentation.settings.appstatus

import app.paralino.core.provider.LogPriority

data class AppStatusDebugScreenState(
    val query: String = "",
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

sealed class AppStatusDebugScreenEvent {

}

sealed class AppStatusDebugScreenUiEvent {
    data object ClearLogs : AppStatusDebugScreenUiEvent()
    data class SearchLogs(val query: String) : AppStatusDebugScreenUiEvent()
}