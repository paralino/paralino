package app.paralino.android.remote.sync

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.paralino.android.notification.SyncNotificationProvider
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
                .onFailure {
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
        } catch (e: Exception) {
            logg.tag("push_notifications").e("Error fetching push notifications", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            SyncNotificationProvider.PushSyncNotification.notificationId,
            SyncNotificationProvider.PushSyncNotification.create(applicationContext),
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