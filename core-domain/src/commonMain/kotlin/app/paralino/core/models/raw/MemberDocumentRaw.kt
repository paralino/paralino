package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MemberDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant?,
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
                createdAt = (map["\$createdAt"] as? String)?.isoWithOffsetToInstant(),
                ownerUserId = map["ownerUserId"] as String,
                groupId = map["groupId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                data = (map["resourceData"] as? String?) ?: "",
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