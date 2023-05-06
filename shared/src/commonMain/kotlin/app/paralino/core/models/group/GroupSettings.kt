package app.paralino.core.models.group

import kotlinx.serialization.Serializable

@Serializable
data class GroupSettings(
    val id: String,
    val ownerUserId: String,
    val groupId: String,
    val integrity: String,
)
