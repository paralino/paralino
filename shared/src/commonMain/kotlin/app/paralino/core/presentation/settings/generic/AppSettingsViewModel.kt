package app.paralino.core.presentation.settings.generic

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.current
import app.paralino.core.repo.base.AppConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppSettingsViewModel(
    private val appPrefs: AppPrefs,
    private val appConfigRepo: AppConfigRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsScreenState())
    val uiState: StateFlow<AppSettingsScreenState> = _uiState.asStateFlow()

    init {
        updateState()
    }

    private fun updateState() {
        _uiState.update {
            it.copy(
                showMapsProvider = DeviceType.current == DeviceType.Android,
                showNavigationApps = DeviceType.current == DeviceType.iOS && IOSNavigationAppType.supportedMaps().size > 1,
                showNotificationSocket = appConfigRepo.hasNotificationSocketService
            )
        }
    }
}
