package app.paralino.core.domain.notifications

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
        val notificationGroupId: String?,
        val title: String,
        val message: String?,
        val timeMillis: Long?,
        val timeInfo: String?,
        val loggerTag: String? = null
    )

    companion object {
        val notificationGroupSystem = "system_messages"
    }
}

abstract class IInfoMessagesNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)

abstract class IPlaceActivityNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)

abstract class IGroupActivityNotificationPresenter(inAppLogger: InAppLogger) :
    IPushNotificationPresenter(inAppLogger)
