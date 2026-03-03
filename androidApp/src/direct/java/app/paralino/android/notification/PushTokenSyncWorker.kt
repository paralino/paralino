package app.paralino.android.notification

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
import app.paralino.android.provider.IUnifiedPushManager
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.getOrThrow
import app.paralino.core.provider.NotificationSocketServiceManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PushTokenSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val components by lazy { UserRepoComponent() }

    override suspend fun doWork(): Result {
        try { setForeground(getForegroundInfo()) }
        catch (_: Exception) { }

        try {
            logg.tag("push_notifications").v("Sync push token or device")
            components.userDataUpdate.registerPushTokenIfNeeded()?.getOrThrow()
            if (components.serviceManager.adjustedPushProvider() == PushProvider.UNIFIED_PUSH) {
                components.unifiedPushManager.refresh()
            }
        } catch (e: Exception) {
            logg.tag("push_notifications").e("Error syncing push token", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            SyncNotificationProvider.PushSyncNotification.notificationId,
            SyncNotificationProvider.PushSyncNotification.create(applicationContext),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        )
    }

    companion object {
        fun createRequest(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PushTokenSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            logg.tag("push_notifications").v("Create token sync request")
            workManager.enqueueUniqueWork(
                "push_token_sync_work",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}

class UserRepoComponent() : KoinComponent {
    val userDataUpdate: UpdateUserDataUseCase by inject()
    val serviceManager: NotificationSocketServiceManager by inject()
    val unifiedPushManager: IUnifiedPushManager by inject()
}