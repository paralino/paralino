package app.paralino.core.repo.base

import app.paralino.core.db.LogsDatabaseHelper
import app.paralino.core.db.logs.accesslog.AccessLogEntity
import app.paralino.core.models.AppHost
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.seconds

class AccessLogDataSource(
    private val database: LogsDatabaseHelper,
    private val appConfig: AppConfigRepository
) {

    suspend fun saveAlertNotificationAccessLog(notificationId: String) {
        val accessLog = AccessLogEntity(
            type = "notification",
            host = appConfig.appHost.key,
            accessId = notificationId,
            timeUTC = nowUTC().toString()
        )
        database.accessLogQueries.insertAccessLog(accessLog)
    }

    suspend fun deleteAlertNotificationAccessLog(notificationId: String) {
        database.accessLogQueries.deleteAccessLog(type = "notification", accessId = notificationId)
    }

    suspend fun canProcessAlertNotification(notificationId: String): Boolean {
        val currentValidLogs = database.accessLogQueries.getAccessLogs(
            type = "notification",
            accessId = notificationId
        )
            .filter { it.accessTimeUTC?.isOlderThan(30.seconds) == false }

        return when (appConfig.appHost) {
            AppHost.MAIN_APP -> {
                currentValidLogs.all { it.appHostType == AppHost.MAIN_APP }
            }

            AppHost.IOS_NOTIFICATION_EXTENSION -> {
                currentValidLogs.all { it.appHostType == AppHost.IOS_NOTIFICATION_EXTENSION }
            }

            AppHost.IOS_LOCATION_EXTENSION -> false
        }
    }

    suspend fun clearAll() {
        database.accessLogQueries.clearAll()
    }
}