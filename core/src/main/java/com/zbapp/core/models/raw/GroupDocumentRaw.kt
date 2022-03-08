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
    val shouldRotateKeys: Boolean,
    val data: String = ""
) {

    fun toPostJson(): Map<String, Any> {
        return buildMap {
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
                shouldRotateKeys = map["shouldRotateKeys"] as Boolean,
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
