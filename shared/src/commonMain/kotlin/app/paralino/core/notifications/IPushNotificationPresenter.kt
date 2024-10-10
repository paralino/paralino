package app.paralino.core.notifications

import app.paralino.core.provider.InAppLogger
import app.paralino.core.provider.InAppLoggerLogType
import app.paralino.core.provider.LogPriority
import kotlinx.datetime.Instant

abstract class IPushNotificationPresenter(
    private val inAppLogger: InAppLogger
) {
    protected abstract fun showPushNotification(payload: Payload)

    fun notify(payload: Payload) {
        if (payload.loggerTag != null) {
            val timeInfo = payload.timeInfo ?: payload.timeMillis?.let { Instant.fromEpochMilliseconds(it).toString() }
            inAppLogger.log(
                type = InAppLoggerLogType.NOTIFICATIONS,
                priority = LogPriority.INFO,
                tag = payload.loggerTag,
                title = listOfNotNull(payload.title, payload.message?.let { "($it)" }).joinToString(separator = " "),
                info = timeInfo
            )
        }

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

abstract class IInfoMessagesNotificationPresenter(inAppLogger: InAppLogger): IPushNotificationPresenter(inAppLogger)
abstract class IPlaceActivityNotificationPresenter(inAppLogger: InAppLogger): IPushNotificationPresenter(inAppLogger)
abstract class IGroupActivityNotificationPresenter(inAppLogger: InAppLogger): IPushNotificationPresenter(inAppLogger)
