package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupCryptoKeyDataRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val version: Long,

    /**
     * group key encrypted with member public key, signed with owner private key
     */
    val encryptedGroupKey: String
) {
    companion object {
        fun from(map: Map<String, Any>): GroupCryptoKeyDataRaw {
            return GroupCryptoKeyDataRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                memberUserId = map["memberUserId"] as String,
                version = (map["version"] as Number).toLong(),
                encryptedGroupKey = map["encryptedGroupKey"] as String
            )
        }
    }
}