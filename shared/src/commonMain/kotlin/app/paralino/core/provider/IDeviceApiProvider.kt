package app.paralino.core.provider

import app.paralino.core.config
import app.paralino.core.connectionStatus
import app.paralino.core.data.db.AppConfig
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.base.DeviceType

abstract class IDeviceApiProvider(
    protected val backendConfig: BackendConfig,
    protected val locationStatusProvider: ILocationStatusProvider,
    protected val connectionStatusProvider: IConnectionStatusProvider
) {

    fun initialize() {
        config = createConfig()

        val logr = Logr
        logr.setLogr(createSystemLogr())
        logg = logr

        if(backendConfig.serverUrl.isBlank() || backendConfig.projectId.isBlank()) {
            logg.e("ERROR", "Server config invalid - missing endpoint or projectId")
        }

        if(config.deviceIdSeed.isBlank())
            config.deviceIdSeed = randomUUID

        locationStatusProvider.refreshLocationStatus()
        connectionStatus = connectionStatusProvider
    }

    abstract val batteryLevel: Int
    abstract val randomUUID: String
    abstract val deviceType: DeviceType
    abstract val appVersionCode: Long
    abstract val defaultDeviceName: String
    abstract fun createSystemLogr(): SystemLogr
    abstract fun createConfig(): AppConfig
    abstract suspend fun logout()
}