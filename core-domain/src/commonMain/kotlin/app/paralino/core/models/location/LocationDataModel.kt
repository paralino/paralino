package app.paralino.core.models.location

import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Serializable
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val endTimeUTC: String? = null,
    val speed: Float? = null,
    val altitude: Float? = null,

    /**
     * Indicate if location data in this model is fake
     * Used when user clears their location so we put dummy data and encrypt it
     */
    val isMasked: Boolean = false,
    val isApproximate: Boolean = false,
    val isInPrivacyZone: Boolean = false,
    val privacyZoneMessage: String? = null,
    val isLikelyInaccurate: Boolean? = null,
) {
    val shouldShowApproximateCircle get() = accuracy > 5f
    val isAccurateForPinType get() = accuracy < 50f
    @Transient
    val timeUTCInstant: Instant? = timeUTC?.fromUTCtoInstant()

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = timeUTCInstant ?: return false
        return pinTime.plus(duration) < nowUTC()
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
    val tz: String? = null,
    val batteryLevel: Int? = null,
    val isCharging: Boolean? = null,
    val network: String? = null,
    val signal: Int? = null,
    val status: String? = null,
) {
    val statusType: MemberDeviceStatusType? get() = MemberDeviceStatusType.fromFullKey(status)
    val networkType: NetworkType? get() = NetworkType.fromKey(network)
    val signalStrength: NetworkSignalStrength? get() = NetworkSignalStrength.fromLevel(signal)
    val onlineStatus: MemberDeviceOnlineStatusType get() {
        return when {
            statusType == MemberDeviceStatusType.NOT_SHARING -> MemberDeviceOnlineStatusType.UNKNOWN
            timeUTC == null -> MemberDeviceOnlineStatusType.UNKNOWN
            isOlderThan(3.hours) -> MemberDeviceOnlineStatusType.UNKNOWN
            isOlderThan(10.minutes) -> MemberDeviceOnlineStatusType.OFFLINE
            isOlderThan(20.seconds) -> MemberDeviceOnlineStatusType.BUSY
            isOlderThan(10.seconds) -> MemberDeviceOnlineStatusType.ONLINE_MAYBE
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
    ONLINE, ONLINE_MAYBE, BUSY, OFFLINE, UNKNOWN;

    val isOnlineOrMaybeOnline: Boolean get() = this == ONLINE || this == ONLINE_MAYBE
}

enum class MemberDeviceStatusType constructor(val id: String) {
    INACTIVE("na"),
    ACTIVE_NO_LOCATION("anl"),
    IDLE_NO_LOCATION("inl"),

    NOT_SHARING("ns"),
    ACTIVE("awl"),
    GPS_OFF("gof"),
    MISSING_PERMISSIONS("mp"),
    BATTERY_SAVER_ON("bson"),
    TOO_MANY_REQUESTS("tmr"),
    THROTTLING("thr"),
    IDLE("iwl"),
    ;

    val fullKey: String = name

    val isDeviceSetupWrong: Boolean get() = when(this) {
        GPS_OFF, MISSING_PERMISSIONS, NOT_SHARING -> true
        else -> false
    }
    val isDeviceSetupWrongOrBatterySaver: Boolean get() = this == BATTERY_SAVER_ON || isDeviceSetupWrong
    val isInBlockedState: Boolean get() = this == NOT_SHARING || isDeviceSetupWrongOrBatterySaver
    val isIdlingOrActive: Boolean get() = isIdling || this == ACTIVE
    val isIdling: Boolean get() = when(this) {
        INACTIVE, BATTERY_SAVER_ON, THROTTLING, IDLE -> true
        else -> false
    }

    companion object {
        fun fromFullKey(key: String?): MemberDeviceStatusType? {
            return entries.firstOrNull { it.fullKey == key }
        }

        fun fromShortId(id: String?): MemberDeviceStatusType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}

data class LatLng(val lat: Double, val lng: Double)