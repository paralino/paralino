package app.paralino.android.fcm

import android.content.Context
import androidx.work.*
import app.paralino.core.config
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.logg
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
            val token = FirebaseMessaging.getInstance().token.await()
            logg.tag("push_notifications").v("Got token from firebase: $token")
            config.pushTokenUnsynced = token
            userDataUpdate.userDataUpdate.registerPushTokenIfNeeded()
        } catch (e: Exception) {
            logg.tag("push_notifications").e("Error syncing push token", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        return Result.success()
    }

    companion object {
        fun createRequest(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PushTokenSyncWorker>()
                .setConstraints(constraints)
                .build()

            logg.tag("push_notifications").v("Create token sync request")
            workManager.enqueueUniqueWork("push_sync_work", ExistingWorkPolicy.KEEP, request)
        }
    }
}

class UserRepoComponent() : KoinComponent {
    val userDataUpdate: UpdateUserDataUseCase by inject()
}