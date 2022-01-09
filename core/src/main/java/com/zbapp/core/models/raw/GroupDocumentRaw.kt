package com.zbapp.core.models.raw

import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerId: String,
    val data: String = "",

    // used for decrypting basic group data
    // encrypted with members public key
    val groupKeys: List<GroupCryptoKeyData>?,

    val members: List<MemberDocumentRaw> = emptyList()
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun toPostJson(): Map<String, Any> {
        return buildMap {
            put("data", data)
            put("groupKeys") {
                groupKeys?.map {
                    BaseRemoteModule.formatter.encodeToString(it)
                }
            }
//            putJsonArray("members") {
//                members.forEach { addJsonObject { it.toPostJson() } }
//            }
        }
    }
}

@Serializable
data class GroupCryptoKeyData constructor(
    val groupId: String,
    val memberId: String,
    // group key encrypted with member public key
    val key: String,
    val version: Int
)