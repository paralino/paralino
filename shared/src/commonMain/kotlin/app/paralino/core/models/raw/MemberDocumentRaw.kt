package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupId: String,

    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val verification: String,

    val data: String,
    val paralinoId: String?,
    val settingsRaw: GroupSettingsRaw?,
    val locationIds: List<String>
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): MemberDocumentRaw {
            return MemberDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                data = (map["resourceData"] as? String?) ?: (map["data"] as? String?) ?: "",
                paralinoId = map["paralinoId"] as? String?,
                verification = map["verification"] as String,
                settingsRaw = null,
                locationIds = map["locationIds"] as List<String>,
            )
        }
    }
}

@Serializable
data class MemberWithLocationsRaw(
    val member: MemberDocumentRaw,
    val locations: List<MemberLocationRaw>
)