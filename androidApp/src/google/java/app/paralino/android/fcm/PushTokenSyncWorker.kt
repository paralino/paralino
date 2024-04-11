package app.paralino.android.fcm

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.paralino.android.notification.SyncNotificationProvider
import app.paralino.core.config
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.logg
import app.paralino.core.models.getOrThrow
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PushTokenSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val userDataUpdate by lazy { UserRepoComponent() }

    override suspend fun doWork(): Result {
        try {
            logg.tag("push_notifications").v("Try to get and sync push token")
            FirebaseApp.initializeApp(applicationContext)
            val token = FirebaseMessaging.getInstance().token.await()
            logg.tag("push_notifications").v("Got token from firebase: $token")
            config.pushTokenUnsynced = token
            userDataUpdate.userDataUpdate.registerPushTokenIfNeeded()?.getOrThrow()
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
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
                // setting as expedited will run as foreground service
//                .setExpedited()
                .build()

            logg.tag("push_notifications").v("Create token sync request")
            workManager.enqueueUniqueWork("push_token_sync_work", ExistingWorkPolicy.KEEP, request)
        }
    }
}

class UserRepoComponent() : KoinComponent {
    val userDataUpdate: UpdateUserDataUseCase by inject()
}