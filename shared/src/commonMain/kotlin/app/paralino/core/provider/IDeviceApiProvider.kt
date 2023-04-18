package app.paralino.core.provider

import app.paralino.core.config
import app.paralino.core.connectionStatus
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.base.DeviceType

abstract class IDeviceApiProvider(
    protected val appConfig: BackendConfig,
    protected val locationStatusProvider: ILocationStatusProvider,
    protected val connectionStatusProvider: IConnectionStatusProvider
) {

    fun initialize() {
        config = createConfig()

        val logr = Logr
        logr.setLogr(createSystemLogr())
        logg = logr

        if(appConfig.serverUrl.isBlank() || appConfig.projectId.isBlank()) {
            logg.e("ERROR", "Server config invalid - missing endpoint or projectId")
        }

        if(config.deviceId.isBlank())
            config.deviceId = randomUUID

        locationStatusProvider.refreshLocationStatus()
        connectionStatus = connectionStatusProvider
    }

    abstract val batteryLevel: Int
    abstract val randomUUID: String
    abstract val deviceType: DeviceType
    abstract val appVersionCode: Long
    abstract fun createSystemLogr(): SystemLogr
    abstract fun createConfig(): IConfig
    abstract suspend fun logout()
}