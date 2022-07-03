package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User generates encryption key K and encrypts it with other members public key
 * Key K will be used to encrypt user location data
 * Each group member will have access to encrypted key K and will have to decrypt it to use it
 * This allows for individual member to control who has access to location data
 *
 * User will also upload this key for themselves - that allows for decryption on other devices user may have
 *
 * Location data encryption is not tied to group encryption
 */
@Serializable
data class GroupMemberCryptoKeyDataRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val ownerUserId: String,
    val version: Long,
    // owner sender key K encrypted with member public key, signed with owner private key
    val encryptedSenderKey: String
) {
    companion object {
        fun from(map: Map<String, Any>): GroupMemberCryptoKeyDataRaw {
            return GroupMemberCryptoKeyDataRaw(
                id = map["\$id"] as String,
                memberUserId = map["memberUserId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                version = map["version"] as Long,
                encryptedSenderKey = map["encryptedSenderKey"] as String
            )
        }
    }
}