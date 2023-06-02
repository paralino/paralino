package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSession constructor(
    val id: String,
    val expire: String,
    val deviceType: String,
)
