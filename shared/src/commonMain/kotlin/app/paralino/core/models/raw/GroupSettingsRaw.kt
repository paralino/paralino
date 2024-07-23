package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupSettingsRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String,
    val memberIntegrity: String,
    val groupIntegrity: String?,
    val memberIntegrityUserKeyVersion: Long,
    val groupIntegrityUserKeyVersion: Long?
) {

    companion object {
        fun from(map: Map<String, Any>): GroupSettingsRaw {
            return GroupSettingsRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                memberIntegrity = map["memberIntegrity"] as String,
                groupIntegrity = map["groupIntegrity"] as? String?,
                memberIntegrityUserKeyVersion = (map["memberIntegrityUserKeyVersion"] as Number).toLong(),
                groupIntegrityUserKeyVersion = (map["groupIntegrityUserKeyVersion"] as? Number)?.toLong()
            )
        }
    }
}
