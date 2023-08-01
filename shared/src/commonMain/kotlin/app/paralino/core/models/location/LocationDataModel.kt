package app.paralino.core.models.location

import kotlinx.serialization.Serializable

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
}

enum class MemberDeviceStatusType {
    INACTIVE,
    NOT_SHARING,
    ACTIVE,
    GPS_OFF,
    MISSING_PERMISSIONS,
    BATTERY_SAVER_ON,
    TOO_MANY_REQUESTS,
    IDLE,
    ;

    val key: String = name

    val isIdling: Boolean get() = when(this) {
        INACTIVE, BATTERY_SAVER_ON, TOO_MANY_REQUESTS, IDLE -> true
        else -> false
    }

    companion object {
        fun fromKey(key: String?): MemberDeviceStatusType? {
            return values().firstOrNull { it.key == key }
        }
    }
}