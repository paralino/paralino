package com.zbapp.core.models.raw

import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerId: String,
    val data: String = "",
    val members: List<MemberDocumentRaw> = emptyList()
) {

    fun toPostString() = BaseRemoteModule.formatter.encodeToString(toPostJson())
    fun toPostJson(): JsonObject {
        return buildJsonObject {
            put("ownerId", ownerId)
            put("data", data)
//            putJsonArray("members") {
//                members.forEach { addJsonObject { it.toPostJson() } }
//            }
        }
    }
}