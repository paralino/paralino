package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalGroupKeyRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val encryptedUserKeys: String,
    val groupId: String,
    val personalGroupKeyVersion: Long
) {

    companion object {
        fun from(map: Map<String, Any>): PersonalGroupKeyRaw {
            return PersonalGroupKeyRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedUserKeys = map["encryptedUserKeys"] as String,
                groupId = map["groupId"] as String,
                personalGroupKeyVersion = (map["personalGroupKeyVersion"] as Number).toLong(),
            )
        }
    }
}