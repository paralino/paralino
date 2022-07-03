package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserFriend(
    val id: String,

    val name: String,
    val imageBase64: String?
)
