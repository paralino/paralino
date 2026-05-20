package app.paralino.core.models.location

import kotlinx.serialization.Serializable

@Serializable
data class PrivacyZoneSharedLocationPayload(
    val lat: Double,
    val lng: Double,
    val zoneRadiusMeters: Int,
    val uploadAccuracyMeters: Int,
)
