package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.android.notification.PushTokenSyncWorker
import app.paralino.android.remote.sync.PushNotificationsSyncWorker
import app.paralino.core.logg
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.user.IPushProviderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PushProviderManager(
    private val appClass: AppClass,
    private val notificationServiceManager: NotificationSocketServiceManager,
    private val unifiedPushManager: IUnifiedPushManager
) : IPushProviderManager {

    override fun syncPushToken() {
        PushTokenSyncWorker.createRequest(appClass)
    }

    override fun syncPushNotifications() {
        PushNotificationsSyncWorker.createRequest(appClass)
    }

    override suspend fun logout() {
        try {
            withContext(Dispatchers.IO) {
                notificationServiceManager.setNotificationPushProvider(provider = null)
                unifiedPushManager.unregisterApp()
            }
        } catch (e: Exception) {
            logg.e("push_notifications", "Error clearing remote push token", e)
        }
    }
}