package app.paralino.core.presentation.settings.general

data class GeneralInfoScreenState(
    val isErrorReportingEnabled: Boolean,
    val appVersion: String
)

sealed interface GeneralInfoScreenStateUiEvent {
    data object ToggleCrashReporting: GeneralInfoScreenStateUiEvent
}