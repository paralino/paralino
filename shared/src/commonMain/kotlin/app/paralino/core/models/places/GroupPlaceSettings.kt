package app.paralino.core.models.places

import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceSettings constructor(
    val id: String,
    val groupId: String,
    val placeId: String,
    val ownerUserId: String,
    val deviceId: String?,
    val alertData: GroupPlaceSettingsData?
)

@Serializable
data class GroupPlaceSettingsData constructor(
    val isActive: Boolean = true,
    val settings: List<GroupPlaceSettingsEntry> = emptyList()
)

@Serializable
data class GroupPlaceSettingsEntry constructor(
    val memberUserId: String,
    val deviceId: String? = null,
    val events: Map<String, Boolean> = emptyMap()
)