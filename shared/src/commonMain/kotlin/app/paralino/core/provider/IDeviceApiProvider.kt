package app.paralino.core.provider

import app.paralino.core.data.db.AppConfig
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

abstract class IDeviceApiProvider(
    val locationStatusProvider: ILocationStatusProvider,
    val connectionStatusProvider: IConnectionStatusProvider
) {

    private var _appLifecycleState = MutableStateFlow(AppLifecycleState())
    val appLifecycleState = _appLifecycleState.asStateFlow()

    val deviceStatus = combine(
        locationStatusProvider.status,
        connectionStatusProvider.status
    ) { locationStatus, connectionStatus ->
        DeviceStatusData(locationStatus, connectionStatus)
    }

    fun onAppActive() {
        logg.tag("app_state").v("App is in foreground")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.ACTIVE,
                wasEverActive = true
            )
        }
    }

    fun onAppInactive() {
        logg.tag("app_state").v("App is inactive")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.INACTIVE
            )
        }
    }

    fun onAppBackground() {
        logg.tag("app_state").v("App is in background")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.BACKGROUND
            )
        }
    }

    val batteryLevel: Int? get() = if (_batteryLevel in 0..100) _batteryLevel else null

    protected abstract val _batteryLevel: Int
    abstract val isCharging: Boolean
    abstract val deviceType: DeviceType
    abstract val appVersionCode: Long
    abstract val appVersion: String
    abstract val defaultDeviceName: String
    abstract fun createSystemLogr(): SystemLogr
    abstract fun createCrashReportingLogr(): SystemLogr
    abstract fun createConfig(): AppConfig
    abstract fun copyToClipboard(data: String)
    abstract suspend fun logout()
}

data class DeviceStatusData(
    val locationStatus: LocationConditionStatus,
    val networkStatus: NetworkConnectionInfo
)

data class AppLifecycleState(
    val previous: AppLifecycleType = AppLifecycleType.DESTROYED,
    val current: AppLifecycleType = AppLifecycleType.DESTROYED,
    val wasEverActive: Boolean = false
)

enum class AppLifecycleType {
    ACTIVE, INACTIVE, BACKGROUND, DESTROYED;

    val activeOrInactive: Boolean get() = this == ACTIVE || this == INACTIVE
}