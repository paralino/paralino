package app.paralino.android.fcm

import app.paralino.android.AppClass
import app.paralino.core.repo.user.IPushNotificationManager
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import loge


class PushNotificationManager(
    private val appClass: AppClass
) : IPushNotificationManager {

    override fun syncPushToken() {
        PushTokenSyncWorker.createRequest(appClass)
    }

    override suspend fun logout() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
            FirebaseInstallations.getInstance().delete().await()
        } catch (e: Exception) {
            loge("push_notifications", "Error clearing FCM push token", e)
        }
    }
}