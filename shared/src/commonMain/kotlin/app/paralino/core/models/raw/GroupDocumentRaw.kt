package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val keyVersion: Long,
    val data: String = ""
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                keyVersion = (map["keyVersion"] as Number).toLong(),
                data = map["data"] as String
            )
        }
    }
}

@Serializable
data class GroupWithMemberRaw constructor(
    val group: GroupDocumentRaw,
    val members: List<MemberWithLocationsRaw>
)
