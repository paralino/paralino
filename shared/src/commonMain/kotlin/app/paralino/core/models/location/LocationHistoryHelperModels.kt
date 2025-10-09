package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class LocationHistoryModelForUser(
    val groupId: String,
    val memberUserId: String,
    val history: List<LocationHistoryModelForDevice>
)

@Serializable
data class LocationHistoryModelForDevice(
    val locationId: String?,
    val entryId: String,
    val dateKey: String,
    val historyKeyVersion: String,
    val isCachedLocation: Boolean,
    @Contextual
    val history: CryptoResource<LocationHistoryModel>
)

@Serializable
data class LocationHistoryPointModelForMember(
    val groupId: String,
    val entryId: Long,
    val localGroupHistoryKeyId: Long?,
    val history: LocationHistoryPointModel
)

@Serializable
data class LocationHistoryPointId(
    val entryId: String,
    val pointId: String
)