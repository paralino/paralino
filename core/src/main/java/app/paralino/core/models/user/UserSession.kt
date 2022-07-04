package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val expire: Long
)
