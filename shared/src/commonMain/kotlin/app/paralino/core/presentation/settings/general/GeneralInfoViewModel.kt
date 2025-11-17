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
            appVersion = deviceApiProvider.appVersionText,
            debugInputText = null
        )
    )
    val uiState: StateFlow<GeneralInfoScreenState> = _uiState
    private var versionInfoTapCount: Int = 0

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
                appPrefs.lastShowedAppRatingDialogAppVersion = 0
                appPrefs.didHaveAccountWelcomeOnboarding = false
                appPrefs.didHaveLocationHistoryIntroOnboarding = false
                appPrefs.didHavePersonalSpaceIntroOnboarding = false
            }

            GeneralInfoScreenStateUiEvent.OnVersionInfoTap -> {
                if (versionInfoTapCount >= 10) {
                    if (_uiState.value.debugInputText != null) {
                        _uiState.update { it.copy(debugInputText = null) }
                    } else {
                        _uiState.update { it.copy(debugInputText = "") }
                    }
                    versionInfoTapCount = 0
                } else {
                    versionInfoTapCount++
                }
            }

            is GeneralInfoScreenStateUiEvent.OnChangeDebugText -> {
                _uiState.update { it.copy(debugInputText = event.content) }
            }

            GeneralInfoScreenStateUiEvent.OnSubmitDebugText -> {
                when (_uiState.value.debugInputText) {
                    "enableDevModeNowPrettyPlease" -> {
                        appPrefs.isDevModeEnabled = true
                    }

                    "revert" -> {
                        appPrefs.isDevModeEnabled = false
                    }
                }
                _uiState.update { it.copy(debugInputText = null) }
            }
        }
    }
}