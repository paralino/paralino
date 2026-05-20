package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupPrivacyZoneRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val userKeyVersion: Long,
    val data: String = "",
) {

    companion object {
        fun from(map: Map<String, Any>): GroupPrivacyZoneRaw {
            return GroupPrivacyZoneRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                userKeyVersion = (map["userKeyVersion"] as Number).toLong(),
                data = (map["resourceData"] as? String?) ?: "",
            )
        }
    }
}
