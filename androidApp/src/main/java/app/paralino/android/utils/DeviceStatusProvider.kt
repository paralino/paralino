package app.paralino.android.utils

import androidx.work.WorkManager
import androidx.work.await
import app.paralino.android.AppClass
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.core.utils.IDeviceStatusProvider
import loge

class DeviceStatusProvider(
    private val appClass: AppClass
): IDeviceStatusProvider {
    override val batteryLevel: Int
        get() = appClass.batterLevel

    override suspend fun logout() {
        try {
            loge("logout", "Canceling all work on logout")
            val workManager = WorkManager.getInstance(appClass)
            workManager.cancelAllWork().await()
        } catch (e: Exception) {
            loge("logout", "Error canceling work requests")
        }
    }
}