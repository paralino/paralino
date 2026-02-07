package app.paralino.core.provider

import app.paralino.core.db.AppPrefs
import app.paralino.core.repo.base.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class NotificationSocketServiceManager(
    private val appPrefs: AppPrefs,
    private val notificationStatus: INotificationStatusProvider,
    private val appConfigRepo: AppConfigRepository
) {
    private val _notificationSocketServiceEnabledFlow = MutableStateFlow(appPrefs.notificationSocketServiceEnabled)


    fun setServiceEnabledState(enabledStatus: Boolean?) {
        appPrefs.notificationSocketServiceEnabled = enabledStatus
        _notificationSocketServiceEnabledFlow.update { enabledStatus }
    }

    fun listenForServiceStatus(): Flow<Boolean> {
        return combine(
            notificationStatus.status,
            _notificationSocketServiceEnabledFlow
        ) { notificationStatus, serviceStatus ->
            isServiceAvailable() && serviceStatus == true && notificationStatus.isNotificationPermissionAllowed
        }
    }

    fun isServiceActive(): Boolean {
        return isServiceAvailable() && isServiceEnabledInSettings() && notificationStatus.currentStatus.isNotificationPermissionAllowed
    }

    fun isServiceAvailable(): Boolean {
        return appConfigRepo.hasNotificationSocketService
    }

    fun isServiceEnabledInSettings(): Boolean {
        return _notificationSocketServiceEnabledFlow.value == true
    }
}