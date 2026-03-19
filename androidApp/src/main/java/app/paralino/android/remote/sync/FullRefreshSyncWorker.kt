package app.paralino.android.remote.sync

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.paralino.android.notification.SyncNotificationProvider
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import kotlin.time.Duration.Companion.hours

class FullRefreshSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val dataSync by lazy { DataSyncComponent() }

    override suspend fun doWork(): Result {
        try { setForeground(getForegroundInfo()) }
        catch (_: Exception) { }

        try {
            logg.tag("refresh").i("Try to sync everything from server")
            dataSync.fullRefresh.refreshEverything(
                forceGroupRefresh = false,
                minimumDelayMillis = 12.hours.inWholeMilliseconds
            )
        } catch (e: Exception) {
            logg.tag("refresh").e("Error syncing data, will try again", e)
            return if (runAttemptCount < 2) Result.retry() else Result.failure()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            SyncNotificationProvider.DataSyncNotification.notificationId,
            SyncNotificationProvider.DataSyncNotification.create(applicationContext),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        )
    }

    companion object {
        private const val workId: String = "full_refresh_sync_work"
        fun cancelWork(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(workId)
        }

        fun createRequest(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<FullRefreshSyncWorker>(
                Duration.ofHours(4),
                Duration.ofMinutes(15)
            )
                .setConstraints(constraints)
                .build()

            logg.tag("refresh").v("Create full refresh sync request")
            workManager.enqueueUniquePeriodicWork(
                workId,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

class DataSyncComponent() : KoinComponent {
    val fullRefresh: RefreshEverythingUseCase by inject()
}