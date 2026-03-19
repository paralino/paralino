package app.paralino.android.notification

import app.paralino.core.logg
import app.paralino.core.repo.user.IPushProviderManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class ParalinoFcmService : FirebaseMessagingService() {

    private val pushNotificationManager: IPushProviderManager by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logg.v("push_notifications", "Got new push token from system")
        pushNotificationManager.syncPushToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        logg.v("push_notifications", "Got new push notification from FCM")
        pushNotificationManager.syncPushNotifications()
    }
}

