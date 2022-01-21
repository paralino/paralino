package com.zbapp.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String?,

    val ownerPublicKey: String,

    val data: String,
    val mutableData: String? = null
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun toPostJson(): Map<String, Any?> {
        return buildMap {
            put("ownerUserId", ownerUserId)
            put("groupId", groupId)
            put("ownerPublicKey", ownerPublicKey)
            put("data", data)
            put("mutableData", mutableData)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): MemberDocumentRaw {
            return MemberDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as? String,
                ownerPublicKey = map["ownerPublicKey"] as String,
                data = map["data"] as String,
                mutableData = map["mutableData"] as? String,
            )
        }
    }
}

@Serializable
data class MemberWithKeyRaw(
    val member: MemberDocumentRaw,
    val key: GroupMemberCryptoKeyDataRaw
)

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
    val memberId: String,
    val groupId: String,
    val ownerMemberId: String,
    val memberUserId: String,
    val time: Long,
    // owner sender key K encrypted with member public key, signed with owner private key
    val encryptedSenderKey: String
) {
    companion object {
        fun from(map: Map<String, Any>): GroupMemberCryptoKeyDataRaw {
            return GroupMemberCryptoKeyDataRaw(
                id = map["\$id"] as String,
                memberId = map["memberId"] as String,
                ownerMemberId = map["ownerMemberId"] as String,
                memberUserId = map["memberUserId"] as String,
                groupId = map["groupId"] as String,
                time = map["time"] as Long,
                encryptedSenderKey = map["encryptedSenderKey"] as String
            )
        }
    }
}