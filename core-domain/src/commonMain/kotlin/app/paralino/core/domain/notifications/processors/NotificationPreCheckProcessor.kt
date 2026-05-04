package app.paralino.core.domain.notifications.processors

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.base.AppConfigRepository

class NotificationPreCheckProcessor(
    private val appConfig: AppConfigRepository,
    private val accessLogSource: AccessLogDataSource,
    private val appPrefs: AppPrefs,
) {

    suspend fun preProcess(
        push: PushNotificationRaw,
        onDelete: (suspend (PushNotificationRaw) -> Unit)?
    ): Boolean {
        if (!isPushForThisDevice(push)) {
            return false
        }

        logg.tag("push_notifications")
            .v("Process ${push.importance} ${push.type} push notification. Expired (${push.isExpired})")

        if (push.isExpired) {
            logg.tag("push_notifications").v("Push notification expired, ignore")
            onDelete?.invoke(push)
            return false
        }

        if (appConfig.appHost == AppHost.MAIN_APP && accessLogSource.canProcessAlertNotification(notificationId = push.id)) {
            accessLogSource.saveAlertNotificationAccessLog(notificationId = push.id)
        }

        return true
    }

    fun isPushForThisDevice(push: PushNotificationRaw): Boolean {
        return push.deviceId.isNullOrBlank() || push.deviceId == appPrefs.deviceId || push.deviceId == appPrefs.sessionId
    }
}