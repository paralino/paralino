package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupSettingsRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String,
    val secretKey: String,
    val integrity: String,
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupSettingsRaw {
            return GroupSettingsRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                secretKey = map["secretKey"] as String,
                integrity = map["integrity"] as String,
            )
        }
    }
}
