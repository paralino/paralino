package app.paralino.android.provider

import android.os.Build
import androidx.work.WorkManager
import androidx.work.await
import app.paralino.android.AppClass
import app.paralino.android.BuildConfig
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.android.utils.extensions.copy
import app.paralino.android.utils.extensions.isBatteryCharging
import app.paralino.core.data.db.AppConfig
import app.paralino.core.data.db.AppConfigProvider
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LogPriority
import app.paralino.core.provider.SystemLogr
import app.paralino.core.utils.exceptions.InternalConnectionException
import io.sentry.Sentry
import java.net.SocketTimeoutException
import java.util.UUID

class DeviceApiProvider constructor(
    private val appClass: AppClass,
    backendConfig: BackendConfig,
    locationStatusProvider: ILocationStatusProvider,
    connectionStatusProvider: IConnectionStatusProvider
): IDeviceApiProvider(backendConfig, locationStatusProvider, connectionStatusProvider) {

    override val deviceType: DeviceType
        get() = DeviceType.Android

    override val batteryLevel: Int
        get() = appClass.batterLevel

    override val isCharging: Boolean
        get() = appClass.isBatteryCharging

    override val randomUUID: String
        get() = UUID.randomUUID().toString()

    override val appVersionCode: Long
        get() = BuildConfig.VERSION_CODE.toLong()

    override val appVersion: String
        get() = BuildConfig.VERSION_NAME

    override val defaultDeviceName: String
        get() = Build.MANUFACTURER

    override fun createSystemLogr(): SystemLogr {
        return if (backendConfig.logToConsole) {
            DebugLogr("Paralino")
        } else {
            object : SystemLogr() {
                override fun systemLog(priority: LogPriority, tag: String?, message: String, t: Throwable?) {
                    if(t != null && backendConfig.logErrorsToRemote && priority == LogPriority.ERROR) {
                        if(t !is SocketTimeoutException && t !is InternalConnectionException)
                            Sentry.captureException(t)
                    }
                }
            }
        }
    }

    override fun createConfig(): AppConfig {
        return AppConfig(AppConfigProvider(appClass))
    }

    override fun copyToClipboard(data: String) {
        appClass.copy(data)
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