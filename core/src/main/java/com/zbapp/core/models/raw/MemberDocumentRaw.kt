package com.zbapp.core.models.raw

import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerId: String,
    val groupId: String?,

    // group member keys, used for decrypting
    val membersAccessKeys: List<GroupMemberCryptoKeyData>?,
    val ownerPublicKey: String?,

    val data: String,
    val mutableData: String? = null
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun toPostJson(): Map<String, Any?> {
        return buildMap {
            put("ownerId", ownerId)
            put("groupId", groupId)
            put("membersAccessKeys") {
                membersAccessKeys?.map {
                    BaseRemoteModule.formatter.encodeToString(it)
                }
            }
            put("data", data)
            put("mutableData", mutableData)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): MemberDocumentRaw {
            return MemberDocumentRaw(
                id = map["\$id"] as String,
                ownerId = map["ownerId"] as String,
                groupId = map["groupId"] as? String,
                membersAccessKeys = (map["ownerPublicKey"] as? List<String>)?.map {
                    BaseRemoteModule.formatter.decodeFromString(it)
                },
                ownerPublicKey = map["ownerPublicKey"] as String,
                data = map["data"] as String,
                mutableData = map["mutableData"] as? String,
            )
        }
    }
}

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
data class GroupMemberCryptoKeyData constructor(
    val groupId: String,
    val ownerId: String,
    val forMemberId: String,
    // owner data encryption key K encrypted with member public key
    val key: String,
    val version: Int
)