package app.paralino.android.utils

import androidx.work.WorkManager
import androidx.work.await
import app.paralino.android.AppClass
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.core.logg
import app.paralino.core.utils.IDeviceApiProvider
import loge
import logw
import java.util.UUID

class DeviceApiProvider(
    private val appClass: AppClass
): IDeviceApiProvider {

    override val batteryLevel: Int
        get() = appClass.batterLevel

    override val randomUUID: String
        get() = UUID.randomUUID().toString()

    override suspend fun logout() {
        try {
            logg.tag("logout").w("Canceling all work on logout")
            val workManager = WorkManager.getInstance(appClass)
            workManager.cancelAllWork().await()
        } catch (e: Exception) {
            loge("logout", "Error canceling work requests")
        }
    }
}