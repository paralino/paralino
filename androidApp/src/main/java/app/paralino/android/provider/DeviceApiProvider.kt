package app.paralino.android.provider

import android.os.Build
import androidx.work.WorkManager
import androidx.work.await
import app.paralino.android.AppClass
import app.paralino.android.BuildConfig
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.android.utils.extensions.copy
import app.paralino.android.utils.extensions.isBatteryCharging
import app.paralino.android.utils.extensions.isLowPowerMode
import app.paralino.core.logg
import app.paralino.core.models.AppFlavor
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.INotificationStatusProvider
import app.paralino.core.provider.SystemLogr
import app.paralino.core.repo.base.AppConfigRepository

class DeviceApiProvider constructor(
    private val appClass: AppClass,
    private val appConfig: AppConfigRepository,
    locationStatusProvider: ILocationStatusProvider,
    connectionStatusProvider: IConnectionStatusProvider,
    notificationStatusProvider: INotificationStatusProvider,
) : IDeviceApiProvider(locationStatusProvider, connectionStatusProvider, notificationStatusProvider) {

    override val _batteryLevel: Int
        get() = appClass.batterLevel

    override val isCharging: Boolean
        get() = appClass.isBatteryCharging

    override val isLowPowerMode: Boolean
        get() = appClass.isLowPowerMode

    override val appVersionCode: Long
        get() = BuildConfig.VERSION_CODE.toLong()

    override val appVersion: String
        get() = BuildConfig.VERSION_NAME

    override val defaultDeviceName: String
        get() = Build.MANUFACTURER

    override val deviceOSVersion: String
        get() = Build.VERSION.SDK_INT.toString()

    override val packageName: String
        get() = BuildConfig.APPLICATION_ID

    override val appFlavor: AppFlavor
        get() = appConfig.appFlavor

    override fun createSystemLogr(): SystemLogr {
        return DebugLogr(enabled = appConfig.logToConsole, tagPrefix = "Paralino")
    }

    override fun copyToClipboard(data: String) {
        appClass.copy(data)
    }

    override fun deviceCornerRadius(): Float {
        return 34f
    }

    override suspend fun logout() {
        try {
            logg.tag("logout").w("Canceling all work on logout")
            val workManager = WorkManager.getInstance(appClass)
            workManager.cancelAllWork().await()
        } catch (e: Exception) {
            logg.e("logout", "Error canceling work requests")
        }
    }
}