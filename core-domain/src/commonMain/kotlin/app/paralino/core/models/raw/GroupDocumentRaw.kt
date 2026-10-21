package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.serialization.SerialName
import kotlin.time.Instant

data class GroupDocumentRaw constructor(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant?,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    val data: String,
    val serverIntegrity: String,
    val schemeVersion: String,
    val planType: String,
    val fallbackPlanType: String,
    val provider: String?,
    val planExpiresAt: Instant?,
    val groupPlans: List<String>?
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupDocumentRaw {
            return GroupDocumentRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as? String)?.isoWithOffsetToInstant(),
                ownerUserId = map["ownerUserId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                data = (map["resourceData"] as? String?) ?: "",
                serverIntegrity = map["serverIntegrity"] as String,
                schemeVersion = map["schemeVersion"] as String,
                planType = map["planType"] as String,
                fallbackPlanType = map["fallbackPlanType"] as String,
                planExpiresAt = (map["planExpiresAt"] as? String)?.isoWithOffsetToInstant(),
                provider = map["provider"] as? String,
                groupPlans = map["groupPlans"] as? List<String>?
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
    val places: List<GroupPlaceWithSettingsRaw>,
    val privacyZones: List<GroupPrivacyZoneRaw> = emptyList(),
)
