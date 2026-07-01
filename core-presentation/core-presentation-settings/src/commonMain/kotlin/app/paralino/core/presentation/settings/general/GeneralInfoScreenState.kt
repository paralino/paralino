package app.paralino.core.presentation.settings.general

data class GeneralInfoScreenState(
    val isErrorReportingEnabled: Boolean,
    val appVersion: String,
    val debugInputText: String?
)

sealed interface GeneralInfoScreenStateUiEvent {
    data object ToggleCrashReporting: GeneralInfoScreenStateUiEvent
    data object ResetAccountWelcomeOnboarding: GeneralInfoScreenStateUiEvent
    data object ResetNewFeaturesOnboarding: GeneralInfoScreenStateUiEvent
    data object OnVersionInfoTap: GeneralInfoScreenStateUiEvent
    data class OnChangeDebugText(val content: String): GeneralInfoScreenStateUiEvent
    data object OnSubmitDebugText: GeneralInfoScreenStateUiEvent
}