package app.paralino.android.fcm

import app.paralino.android.notification.PushNotificationHandler
import app.paralino.core.repo.user.IPushProviderManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import logv
import org.koin.android.ext.android.inject

class ParalinoFcmService : FirebaseMessagingService() {

    private val pushNotificationManager: IPushProviderManager by inject()
    private val pushHandler: PushNotificationHandler by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logv("push_notifications", "Got new token in background: $token")
        pushNotificationManager.syncPushToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        logv("push_notifications", "Got new push notification")
        pushHandler.processFcmMessage(message.data)
    }

}

