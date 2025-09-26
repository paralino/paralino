package app.paralino.core.presentation.settings.general

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GeneralInfoViewModel(
    private val appPrefs: AppPrefs,
    private val deviceApiProvider: IDeviceApiProvider
): CoreViewModel() {
    private val _uiState = MutableStateFlow(
        GeneralInfoScreenState(
            isErrorReportingEnabled = appPrefs.errorReporting,
            appVersion = deviceApiProvider.appVersionText
        )
    )
    val uiState: StateFlow<GeneralInfoScreenState> = _uiState

    fun onEvent(event: GeneralInfoScreenStateUiEvent) {
        when (event) {
            GeneralInfoScreenStateUiEvent.ToggleCrashReporting -> {
                _uiState.update {
                    val newValue = !it.isErrorReportingEnabled
                    logg.v("Toggle error reporting: $newValue")
                    appPrefs.errorReporting = newValue
                    it.copy(isErrorReportingEnabled = newValue)
                }
            }

            GeneralInfoScreenStateUiEvent.ResetAccountWelcomeOnboarding -> {
                appPrefs.didHaveAccountWelcomeOnboarding = false
                appPrefs.didHaveLocationHistoryIntroOnboarding = false
            }
        }
    }
}