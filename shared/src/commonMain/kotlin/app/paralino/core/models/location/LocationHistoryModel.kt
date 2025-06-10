package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class LocationHistoryModel(
    val history: List<LocationHistoryPointModel>
)

@Serializable
data class LocationHistoryModelForUser(
    val groupId: String,
    val memberUserId: String,
    val history: List<LocationHistoryModelForDevice>
)

@Serializable
data class LocationHistoryModelForDevice(
    val locationId: String?,
    @Contextual
    val history: CryptoResource<LocationHistoryModel>
)

@Serializable
data class LocationHistoryPointModelForMember(
    val groupId: String,
    val entryId: Long,
    val localHistoryKeyId: Long,
    val history: LocationHistoryPointModel
)

@Serializable
data class LocationHistoryPointModel(
    val lat: Double,
    val lng: Double,
    val acc: Float = 0f,
    val timeUTC: String,
    val speed: Float? = null,
    val alt: Float? = null,
    val bat: Int? = null,
    val charging: Boolean? = null,
    val network: String? = null,
    val signal: Int? = null,
) {
    val dayKey = timeUTC.isoToLocalDateTime()?.date?.toString()
}
