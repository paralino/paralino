package com.zbapp.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerId: String,
    val groupId: String?,
    val data: String,
    val mutableData: String? = null
) {

    fun toPostJson(): JsonObject {
        return buildJsonObject {
            put("ownerId", ownerId)
            put("groupId", groupId)
            put("data", data)
            put("mutableData", mutableData)
        }
    }
}