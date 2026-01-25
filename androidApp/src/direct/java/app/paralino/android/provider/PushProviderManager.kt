package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.android.remote.sync.PushNotificationsSyncWorker
import app.paralino.core.logg
import app.paralino.core.repo.user.IPushProviderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PushProviderManager(
    private val appClass: AppClass
) : IPushProviderManager {

    override fun syncPushToken() {
        // no push token in direct version
    }

    override fun syncPushNotifications() {
        PushNotificationsSyncWorker.createRequest(appClass)
    }

    override suspend fun logout() {
        try {
            withContext(Dispatchers.IO) {
                // TODO clear token from push provider if possible
            }
        } catch (e: Exception) {
            logg.e("push_notifications", "Error clearing remote push token", e)
        }
    }
}