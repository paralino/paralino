package app.paralino.core.presentation.settings.generic

data class AppSettingsScreenState(
    val showMapsProvider: Boolean = false,
    val showNavigationApps: Boolean = false,
    val showNotificationSocket: Boolean = false
)

sealed interface AppSettingsScreenUiEvent {
    data object MeasurementSystemClick : AppSettingsScreenUiEvent
    data object NotificationGroupingClick : AppSettingsScreenUiEvent
    data object NavigationAppClick : AppSettingsScreenUiEvent
    data object PlaceSearchSettingsClick : AppSettingsScreenUiEvent
    data object NotificationSocketSettingsClick : AppSettingsScreenUiEvent
    data object MapSourceSettingsClick : AppSettingsScreenUiEvent
}
