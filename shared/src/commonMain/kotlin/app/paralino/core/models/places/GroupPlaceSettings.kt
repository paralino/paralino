package app.paralino.core.models.places

import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceSettings constructor(
    val id: String,
    val groupId: String,
    val placeId: String,
    val ownerUserId: String,
    val alertData: List<GroupPlaceSettingsData>,
)

@Serializable
data class GroupPlaceSettingsData constructor(
    val memberUserId: String,
    val events: Map<String, Boolean> = emptyMap()
)