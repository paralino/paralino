package app.paralino.core.models.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null,
    val speed: Float? = null,
    val altitude: Float? = null
) {
    val shouldShowApproximateCircle get() = accuracy > 10f
}