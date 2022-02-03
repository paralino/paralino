package com.zbapp.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val data: String = ""
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun toPostJson(): Map<String, Any> {
        return buildMap {
            put("ownerUserId", ownerUserId)
            put("ownerPublicKey", ownerPublicKey)
            put("data", data)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                ownerPublicKey = map["ownerPublicKey"] as String,
                keyVersion = map["keyVersion"] as Long,
                data = map["data"] as String
            )
        }
    }
}

@Serializable
data class GroupWithMemberRaw constructor(
    val group: GroupDocumentRaw,
    val members: List<MemberDocumentRaw>
)

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
                version = map["version"] as Long,
                encryptedGroupKey = map["encryptedGroupKey"] as String
            )
        }
    }
}

@Serializable
data class CreateGroupPostData constructor(
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedMemberKey: String,
    val groupData: String,
    val memberData: String
)

@Serializable
data class CreateGroupMemberFunctionData constructor(
    val groupId: String,
    val userId: String,
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedOwnerKey: String,
    val memberData: String
)

@Serializable
data class DeleteGroupMemberFunctionData constructor(
    val groupId: String,
    val userId: String
)