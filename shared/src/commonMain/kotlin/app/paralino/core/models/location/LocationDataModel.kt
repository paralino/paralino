package app.paralino.core.models.location

import app.paralino.core.models.data
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val endTimeUTC: String? = null,
    val batteryLevel: Int? = null,
    val speed: Float? = null,
    val altitude: Float? = null
) {
    val shouldShowApproximateCircle get() = accuracy > 2f

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = timeUTC?.fromUTCtoInstant() ?: return false
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
    val batteryLevel: Int? = null,
    val status: String? = null,
) {
    val statusType: MemberDeviceStatusType?
        get() = MemberDeviceStatusType.fromKey(status)

    val validBatteryLevel: Int?
        get() {
            batteryLevel ?: return null
            if(statusType == MemberDeviceStatusType.NOT_SHARING) return null
            if(statusType == MemberDeviceStatusType.NOT_SHARING) return null
            if(timeUTC == null) return null
            if(timeUTC.fromUTCtoInstant()?.plus(2.minutes)?.let { it > nowUTC() } != true) return null
            return batteryLevel
        }
}

enum class MemberDeviceStatusType {
    INACTIVE,
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