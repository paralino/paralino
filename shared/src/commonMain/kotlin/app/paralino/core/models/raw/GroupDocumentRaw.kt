package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    val data: String,
    val serverIntegrity: String,
    val planType: String,
    val schemeVersion: String,
    val membersLimitOverride: Long?,
    val devicesLimitOverride: Long?,
    val placesLimitOverride: Long?
) {

    companion object {
        fun from(map: Map<String, Any>): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                data = map["data"] as String,
                serverIntegrity = map["serverIntegrity"] as String,
                planType = map["planType"] as String,
                schemeVersion = map["schemeVersion"] as String,
                membersLimitOverride = (map["membersLimitOverride"] as? Number?)?.toLong(),
                devicesLimitOverride = (map["devicesLimitOverride"] as? Number?)?.toLong(),
                placesLimitOverride = (map["placesLimitOverride"] as? Number?)?.toLong()
            )
        }
    }
}

@Serializable
data class GroupWithMembersRaw constructor(
    val group: GroupDocumentRaw,
    val members: List<MemberWithLocationsRaw>
)
