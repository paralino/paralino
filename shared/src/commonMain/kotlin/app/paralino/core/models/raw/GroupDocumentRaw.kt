package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.serialization.SerialName
import kotlin.time.Instant

data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    val data: String,
    val serverIntegrity: String,
    val schemeVersion: String,
    val planType: String,
    val fallbackPlanType: String,
    val provider: String?,
    val planExpiresAt: Instant?,
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
                schemeVersion = map["schemeVersion"] as String,
                planType = map["planType"] as String,
                fallbackPlanType = map["fallbackPlanType"] as String,
                planExpiresAt = (map["planExpiresAt"] as? String)?.isoWithOffsetToInstant(),
                provider = map["provider"] as? String,
                membersLimitOverride = (map["membersLimitOverride"] as? Number?)?.toLong(),
                devicesLimitOverride = (map["devicesLimitOverride"] as? Number?)?.toLong(),
                placesLimitOverride = (map["placesLimitOverride"] as? Number?)?.toLong()
            )
        }
    }
}

data class GroupWithMembersRaw constructor(
    val group: GroupDocumentRaw,
    val members: List<MemberWithLocationsRaw>
)

data class GroupWithMembersAndPlacesRaw constructor(
    val group: GroupWithMembersRaw,
    val places: List<GroupPlaceWithSettingsRaw>
)
