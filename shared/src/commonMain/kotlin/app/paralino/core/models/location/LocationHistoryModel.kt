package app.paralino.core.models.location

import kotlinx.serialization.Serializable

@Serializable
data class LocationHistoryModel(
    val history: List<LocationHistoryPointModel>
)

@Serializable
data class LocationHistoryPointModel(
    val lat: Double,
    val lng: Double,
    val acc: Float = 0f,
    val timeUTC: String? = null,
    val speed: Float? = null,
    val alt: Float? = null,
    val bat: Int? = null,
    val charging: Boolean? = null,
    val network: String? = null,
    val signal: Int? = null,
)
