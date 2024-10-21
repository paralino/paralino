package app.paralino.core.notifications

import app.paralino.core.provider.InAppLogger

abstract class IPushNotificationPresenter(
    private val inAppLogger: InAppLogger
) {
    protected abstract fun showPushNotification(payload: Payload)

    fun notify(payload: Payload) {
        inAppLogger.logNotificationPayload(payload)
        showPushNotification(payload)
    }

    data class Payload(
        val notificationId: Int,
        val title: String,
        val message: String?,
        val timeMillis: Long?,
        val timeInfo: String?,
        val loggerTag: String? = null
    )
}

abstract class IInfoMessagesNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)

abstract class IPlaceActivityNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)

abstract class IGroupActivityNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)
