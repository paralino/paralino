package app.paralino.core.models.places

import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceSettings constructor(
    val id: String,
    val groupId: String,
    val placeId: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val data: GroupPlaceSettingsData
)

@Serializable
data class GroupPlaceSettingsData constructor(
    val data: String
)