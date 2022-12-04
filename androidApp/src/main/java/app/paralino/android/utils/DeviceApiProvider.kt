package app.paralino.android.utils

import androidx.work.WorkManager
import androidx.work.await
import app.paralino.android.AppClass
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import app.paralino.core.utils.IConfig
import app.paralino.core.utils.IDeviceApiProvider
import loge
import java.util.*

class DeviceApiProvider constructor(
    private val appClass: AppClass,
    private val config: IConfig
): IDeviceApiProvider {

    override fun initialize() {
        if(config.deviceId.isBlank())
            config.deviceId = randomUUID
    }

    override val deviceType: DeviceType
        get() = DeviceType.Android

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