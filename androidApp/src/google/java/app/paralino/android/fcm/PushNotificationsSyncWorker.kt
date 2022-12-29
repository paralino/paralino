package app.paralino.android.fcm

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.*
import app.paralino.android.notification.PushSyncNotificationHandler
import app.paralino.core.logg
import app.paralino.core.notifications.PushNotificationHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PushNotificationsSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val pushHandler by lazy { PushHandlerComponent() }

    override suspend fun doWork(): Result {
        try {
            logg.tag("push_notifications").v("Try to get push notifications from server")
            pushHandler.pushHandler.checkForPushNotifications()
        } catch (e: Exception) {
            logg.tag("push_notifications").e("Error fetching push notifications", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            PushSyncNotificationHandler.notificationId,
            PushSyncNotificationHandler.create(applicationContext),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        fun createRequest(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PushNotificationsSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            logg.tag("push_notifications").v("Create push notifications sync request")
            workManager.enqueueUniqueWork("push_notifications_sync_work", ExistingWorkPolicy.KEEP, request)
        }
    }
}

class PushHandlerComponent() : KoinComponent {
    val pushHandler: PushNotificationHandler by inject()
}