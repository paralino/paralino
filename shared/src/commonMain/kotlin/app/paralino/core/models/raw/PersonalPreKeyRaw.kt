package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalPreKeyRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val encryptedUserKeys: String,
    val integrity: String,
    val userKeyVersion: Long?,
    val encryptedCodeKeyData: String?,
) {

    companion object {
        fun from(map: Map<String, Any>): PersonalPreKeyRaw {
            return PersonalPreKeyRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedUserKeys = map["encryptedUserKeys"] as String,
                integrity = map["integrity"] as String,
                encryptedCodeKeyData = map["encryptedCodeKeyData"] as? String?,
                userKeyVersion = (map["userKeyVersion"] as? Number?)?.toLong()
            )
        }
    }
}