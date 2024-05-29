package app.paralino.core.models.location

import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val endTimeUTC: String? = null,
    val speed: Float? = null,
    val altitude: Float? = null
) {
    val shouldShowApproximateCircle get() = accuracy > 2f
    val isAccurate get() = accuracy < 20f
    val timeUTCInstant: Instant? = timeUTC?.fromUTCtoInstant()

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = timeUTCInstant ?: return false
        return pinTime.plus(duration) < nowUTC()
    }

    companion object {
        fun getBetter(first: LocationDataModel?, second: LocationDataModel?): LocationDataModel? {
            if (second?.timeUTCInstant == null) {
                return first
            } else if (first?.timeUTCInstant != null) {
                // if new location is older than currently pending one, discard it
                if (first.timeUTCInstant > second.timeUTCInstant) {
                    // first is newer, second is older
                    val difference = first.timeUTCInstant - second.timeUTCInstant
                    if (difference < 1.seconds && second.accuracy < first.accuracy) {
                        // time difference is not huge but accuracy is better, use second location
                        return second
                    } else {
                        return first
                    }
                } else if (second.timeUTCInstant > first.timeUTCInstant) {
                    val difference = second.timeUTCInstant - first.timeUTCInstant
                    if (difference < 1.seconds && first.accuracy < second.accuracy) {
                        // time difference is not huge but accuracy is better, use first location
                        return first
                    } else {
                        return second
                    }
                } else {
                    return if (first.accuracy < second.accuracy) first else second
                }
            } else {
                return second
            }
        }
    }
}

@Serializable
data class MemberDeviceData constructor(
    val deviceName: String,
    val deviceType: String
)

@Serializable
data class MemberDeviceStatus constructor(
    val timeUTC: String? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean? = null,
    val network: String? = null,
    val signal: Int? = null,
    val status: String? = null,
) {
    val statusType: MemberDeviceStatusType? get() = MemberDeviceStatusType.fromKey(status)
    val networkType: NetworkType? get() = NetworkType.fromKey(network)
    val signalStrength: NetworkSignalStrength? get() = NetworkSignalStrength.fromLevel(signal)
    val onlineStatus: MemberDeviceOnlineStatusType get() {
        return when {
            statusType == MemberDeviceStatusType.NOT_SHARING -> MemberDeviceOnlineStatusType.UNKNOWN
            timeUTC == null -> MemberDeviceOnlineStatusType.UNKNOWN
            isOlderThan(3.hours) -> MemberDeviceOnlineStatusType.UNKNOWN
            isOlderThan(10.minutes) -> MemberDeviceOnlineStatusType.OFFLINE
            isOlderThan(1.minutes) -> MemberDeviceOnlineStatusType.BUSY
            else -> MemberDeviceOnlineStatusType.ONLINE
        }
    }

    fun validBatteryLevel(checkForTime: Boolean): Int? {
            batteryLevel ?: return null
            if(statusType == MemberDeviceStatusType.NOT_SHARING) return null
            if(timeUTC == null) return null
            if(checkForTime && isOlderThan(30.minutes)) return null
            return batteryLevel
        }

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = timeUTC?.fromUTCtoInstant() ?: return false
        return pinTime.plus(duration) < nowUTC()
    }
}

enum class MemberDeviceOnlineStatusType {
    ONLINE, BUSY, OFFLINE, UNKNOWN
}

enum class MemberDeviceStatusType {
    INACTIVE,
    ACTIVE_NO_LOCATION,
    IDLE_NO_LOCATION,

    NOT_SHARING,
    ACTIVE,
    GPS_OFF,
    MISSING_PERMISSIONS,
    BATTERY_SAVER_ON,
    TOO_MANY_REQUESTS,
    THROTTLING,
    IDLE,
    ;

    val key: String = name

    val isDeviceSetupWrong: Boolean get() = when(this) {
        GPS_OFF, MISSING_PERMISSIONS, BATTERY_SAVER_ON, NOT_SHARING -> true
        else -> false
    }
    val isInBlockedState: Boolean get() = this == NOT_SHARING || isDeviceSetupWrong
    val isIdlingOrActive: Boolean get() = isIdling || this == ACTIVE
    val isIdling: Boolean get() = when(this) {
        INACTIVE, BATTERY_SAVER_ON, THROTTLING, IDLE -> true
        else -> false
    }

    companion object {
        fun fromKey(key: String?): MemberDeviceStatusType? {
            return values().firstOrNull { it.key == key }
        }
    }
}