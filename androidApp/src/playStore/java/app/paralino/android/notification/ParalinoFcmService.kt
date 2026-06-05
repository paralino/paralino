package app.paralino.android.notification

import app.paralino.core.logg
import app.paralino.core.provider.InAppLogger
import app.paralino.core.repo.user.IPushProviderManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class ParalinoFcmService : FirebaseMessagingService() {

    private val pushNotificationManager: IPushProviderManager by inject()
    private val inAppLogger: InAppLogger by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logg.v("push_notifications", "Got new FCM push token from system")
        pushNotificationManager.syncPushToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val isDeprioritized = message.priority == RemoteMessage.PRIORITY_NORMAL && message.originalPriority == RemoteMessage.PRIORITY_HIGH
        logg.v("push_notifications", "Got new ${if (isDeprioritized) "DEPRIORITIZED" else "prio: ${message.priority}"} push notification from FCM")

        pushNotificationManager.syncPushNotifications()

        if (isDeprioritized) {
            inAppLogger.logDeprioritizedNotification(tag = "FCM")
        }
    }
}

