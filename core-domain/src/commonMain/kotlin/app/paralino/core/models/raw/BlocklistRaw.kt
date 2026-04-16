package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlocklistRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val resourceId: String,
    val resourceType: String,
    val data: String?,
    val userKeyVersion: Long?
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): BlocklistRaw {
            return BlocklistRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                resourceId = map["resourceId"] as String,
                resourceType = map["resourceType"] as String,
                data = (map["resourceData"] as? String?) ?: (map["data"] as? String?),
                userKeyVersion = (map["userKeyVersion"] as? Number?)?.toLong()
            )
        }
    }
}