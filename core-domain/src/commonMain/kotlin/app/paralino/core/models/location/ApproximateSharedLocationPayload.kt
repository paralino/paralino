package app.paralino.core.models.location

import kotlinx.serialization.Serializable

@Serializable
data class ApproximateSharedLocationPayload(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val adjustedRadiusMeters: Int
)
