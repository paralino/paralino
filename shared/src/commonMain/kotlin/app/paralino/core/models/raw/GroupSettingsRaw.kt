package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupSettingsRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String,
    val encryptedUserKeys: String,
    val integrity: String,
) {

    companion object {
        fun from(map: Map<String, Any>): GroupSettingsRaw {
            return GroupSettingsRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                encryptedUserKeys = map["encryptedUserKeys"] as String,
                integrity = map["integrity"] as String,
            )
        }
    }
}
