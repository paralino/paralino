package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class GroupDocumentRaw constructor(
    val id: String,
    val ownerId: String?,
    val data: String,
    val members: List<MemberDocumentRaw>
) {

    fun toPostJson(): JsonObject {
        return buildJsonObject {
            put("ownerId", ownerId)
            put("data", data)
            putJsonArray("members") {
                members.forEach { addJsonObject { it.toPostJson() } }
            }
        }
    }
}