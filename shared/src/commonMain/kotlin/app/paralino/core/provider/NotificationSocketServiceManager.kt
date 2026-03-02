package app.paralino.core.provider

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.base.PushProvider
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
    private val _notificationPushProviderFlow = MutableStateFlow(appPrefs.notificationPushProvider)


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