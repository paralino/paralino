package app.paralino.core.models.places

import app.paralino.core.models.base.ImageModel
import kotlinx.serialization.Serializable

@Serializable
data class GroupPlace constructor(
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val keyVersion: Long,
    val private: Boolean,
    val data: GroupPlaceData
)

@Serializable
data class GroupPlaceData constructor(
    val name: String,
    val image: ImageModel,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val expiresAt: String,
    val type: String,
)

@Serializable
data class GroupPlaceWithSettings constructor(
    val place: GroupPlace,
    val settings: GroupPlaceSettings?
)

data class GroupPlaceGeofenceData constructor(
    val placeId: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int
)
