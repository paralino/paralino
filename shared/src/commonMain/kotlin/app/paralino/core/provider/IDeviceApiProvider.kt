package app.paralino.core.provider

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.base.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

abstract class IDeviceApiProvider(
    val locationStatusProvider: ILocationStatusProvider,
    val connectionStatusProvider: IConnectionStatusProvider,
    val notificationStatusProvider: INotificationStatusProvider
) {

    private var _appLifecycleState = MutableStateFlow(AppLifecycleState())
    val appLifecycleState = _appLifecycleState.asStateFlow()

    val deviceStatus = combine(
        locationStatusProvider.status,
        connectionStatusProvider.status,
        notificationStatusProvider.status
    ) { locationStatus, connectionStatus, notificationStatus ->
        DeviceStatusData(locationStatus, connectionStatus, notificationStatus)
    }

    fun currentDeviceStatus(): DeviceStatusData {
        return DeviceStatusData(
            locationStatus = locationStatusProvider.currentStatus,
            networkStatus = connectionStatusProvider.currentStatus,
            notificationStatus = notificationStatusProvider.currentStatus
        )
    }

    fun onAppActive() {
        logg.tag("app_state").i("App is in foreground")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.ACTIVE,
                wasEverActive = true,
                cameFromBackground = it.wasInBackground,
                wasInBackground = false
            )
        }
    }

    fun onAppInactive() {
        logg.tag("app_state").i("App is inactive")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.INACTIVE,
                cameFromBackground = it.wasInBackground
            )
        }
    }

    fun onAppBackground() {
        logg.tag("app_state").i("App is in background")
        _appLifecycleState.update {
            it.copy(
                previous = it.current,
                current = AppLifecycleType.BACKGROUND,
                wasInBackground = true
            )
        }
    }

    val batteryLevel: Int? get() = if (_batteryLevel in 0..100) _batteryLevel else null
    val clientHeaderValue: String get() = "${packageName}/${deviceOSVersion}/${appVersionCode}/${appVersion}"

    protected abstract val _batteryLevel: Int
    abstract val isCharging: Boolean
    val deviceType: DeviceType get() = DeviceType.current
    abstract val deviceOSVersion: Int
    abstract val appVersionCode: Long
    abstract val appVersion: String
    abstract val defaultDeviceName: String
    abstract val packageName: String
    abstract fun createSystemLogr(): SystemLogr
    abstract fun copyToClipboard(data: String)
    abstract suspend fun logout()
}

data class DeviceStatusData(
    val locationStatus: LocationConditionStatus,
    val networkStatus: NetworkConnectionInfo,
    val notificationStatus: NotificationConditionStatus
)

data class AppLifecycleState(
    val previous: AppLifecycleType = AppLifecycleType.DESTROYED,
    val current: AppLifecycleType = AppLifecycleType.DESTROYED,
    val wasEverActive: Boolean = false,

    // When coming from background iOS has Background > Inactive > Active lifecycle
    // so we can not simply check for previous state so we are keeping a flag is app was actually in background
    // flag is reset and consumed when app comes to foreground
    val wasInBackground: Boolean = false,
    private val cameFromBackground: Boolean = false
) {
    val comingFromBackground get() = wasEverActive && current == AppLifecycleType.ACTIVE && cameFromBackground
}

enum class AppLifecycleType {
    ACTIVE, INACTIVE, BACKGROUND, DESTROYED;

    val activeOrInactive: Boolean get() = this == ACTIVE || this == INACTIVE
}

expect val DeviceType.Companion.current: DeviceType