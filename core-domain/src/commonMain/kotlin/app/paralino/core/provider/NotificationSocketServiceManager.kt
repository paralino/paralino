package app.paralino.core.provider

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.base.PushServiceConnectionState
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class NotificationSocketServiceManager(
    private val appPrefs: AppPrefs,
    private val notificationStatus: INotificationStatusProvider,
    private val appConfigRepo: AppConfigRepository
) {
    private val _notificationPushProviderFlow = MutableStateFlow(appPrefs.notificationPushProvider)

    private val _connectionState = MutableStateFlow<PushServiceConnectionState?>(null)
    val connectionState: StateFlow<PushServiceConnectionState?> = _connectionState.asStateFlow()

    val connectionType: ParalinoSocketConnectionType
        get() = appPrefs.paralinoSocketConnectionType

    fun setConnectionState(state: PushServiceConnectionState?) {
        _connectionState.update { state }
    }

    fun setNotificationPushProvider(provider: PushProvider?) {
        appPrefs.notificationPushProvider = provider
        _notificationPushProviderFlow.update { provider }
    }

    fun listenForServiceStatus(): Flow<Boolean> {
        return combine(
            notificationStatus.status,
            _notificationPushProviderFlow
        ) { notificationStatus, provider ->
            provider.isServiceEnabledInSettings() && notificationStatus.isNotificationPermissionAllowed
        }
    }

    fun isServiceActive(): Boolean {
        return isServiceAvailableAndEnabled() && notificationStatus.currentStatus.isNotificationPermissionAllowed
    }

    fun isServiceAvailableAndEnabled(): Boolean {
        return hasNotificationSocketService() && isServiceEnabledInSettings()
    }

    fun hasNotificationSocketService(): Boolean {
        return appConfigRepo.hasNotificationSocketService
    }

    fun isServiceEnabledInSettings(): Boolean {
        return _notificationPushProviderFlow.value.isServiceEnabledInSettings()
    }

    fun getServiceSetupError(): ConditionErrorType.PushNotificationService? {
        val provider = _notificationPushProviderFlow.value
        return when {
            !hasNotificationSocketService() -> null
            provider.isServiceEnabledInSettings() -> {
                if (appPrefs.paralinoSocketConnectionType == ParalinoSocketConnectionType.REALTIME) {
                    null
                } else {
                    ConditionErrorType.PushNotificationService.PushNotificationServiceNoConstantConnection
                }
            }
            provider == PushProvider.UNIFIED_PUSH && !appPrefs.selectedUnifiedPushDistributor.isNullOrBlank() -> null
            else -> ConditionErrorType.PushNotificationService.PushNotificationServiceOff
        }
    }

    private fun PushProvider?.isServiceEnabledInSettings(): Boolean {
        val provider = this
        if (hasNotificationSocketService()) {
            return provider == PushProvider.PARALINO || provider == PushProvider.DEVICE_NATIVE
        } else {
            return false
        }
    }

    fun adjustedPushProvider(): PushProvider {
        return when (appPrefs.notificationPushProvider) {
            PushProvider.PARALINO -> {
                if (isServiceActive()) {
                    PushProvider.PARALINO
                } else {
                    PushProvider.NOT_SET
                }
            }

            null,
            PushProvider.DEVICE_NATIVE -> {
                if (appConfigRepo.appFlavor == AppFlavor.ANDROID_DIRECT) {
                    if (isServiceActive()) {
                        PushProvider.PARALINO
                    } else {
                        PushProvider.NOT_SET
                    }
                } else {
                    PushProvider.DEVICE_NATIVE
                }
            }
            PushProvider.UNIFIED_PUSH -> PushProvider.UNIFIED_PUSH
            PushProvider.NOT_SET -> PushProvider.NOT_SET
        }
    }
}