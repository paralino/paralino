package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.android.notification.PushTokenSyncWorker
import app.paralino.android.remote.sync.PushNotificationsSyncWorker
import app.paralino.core.logg
import app.paralino.core.repo.user.IPushProviderManager
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class PushProviderManager(
    private val appClass: AppClass
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
                FirebaseMessaging.getInstance().deleteToken().await()
                FirebaseInstallations.getInstance().delete().await()
            }
        } catch (e: Exception) {
            logg.e("push_notifications", "Error clearing FCM push token", e)
        }
    }
}