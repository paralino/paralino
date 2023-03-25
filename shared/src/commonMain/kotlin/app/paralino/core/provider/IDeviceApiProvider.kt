package app.paralino.core.provider

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType

abstract class IDeviceApiProvider {

    fun initialize() {
        config = createConfig()

        val logr = Logr
        logr.setLogr(createSystemLogr())
        logg = logr

        if(config.deviceId.isBlank())
            config.deviceId = randomUUID
    }

    abstract val batteryLevel: Int
    abstract val randomUUID: String
    abstract val deviceType: DeviceType
    abstract val appVersionCode: Long
    abstract fun createSystemLogr(): SystemLogr
    abstract fun createConfig(): IConfig
    abstract suspend fun logout()
}