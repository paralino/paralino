package app.paralino.core.models.group

import app.paralino.core.appPrefs
import app.paralino.core.db.main.groups.MemberRegisteredLocationCount
import app.paralino.core.domain.crypto.group.VerificationResult
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    @Contextual
    val data: CryptoResource<UserGroupData>,
    val serverIntegrity: String,
    val schemeVersion: String,
    val planType: String,
    val fallbackPlanType: String,
    val planExpiresAt: Instant?,
    val provider: String?,
    val membersLimitOverride: Long?,
    val devicesLimitOverride: Long?,
    val placesLimitOverride: Long?,
    val createdAt: Instant?
) {

    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = data.data?.name
    val image: ImageModel get() = data.data?.image ?: ImageModel.emoji(Emojis.lock)
    val isCurrentUserOwner: Boolean get() = ownerUserId == appPrefs.userId
    val enforceMemberAuthentication: Boolean = data.data?.enforceMemberAuthentication != false

    // allow for some manual grace period
    // it might take few minutes until correct subscription data is synced and reflected on backend
    private val isPlanExpired: Boolean get() = planExpiresAt
        ?.plus(if (AppBuildConfig.instance.isDevMode) 30.seconds else 2.minutes)
        ?.let { it < nowUTC() }
        ?: false

    fun isGroupPremium(config: ConfigData): Boolean {
        return planConfig(config).isPremium
    }

    fun membersLimit(config: ConfigData): Int {
        if (isPlanExpired) {
            return config.planConfigForPlanTypeOrDefault(fallbackPlanType).membersLimit
        } else {
            return membersLimitOverride?.toInt()
                ?: config.planConfigForPlanTypeOrDefault(planType).membersLimit
        }
    }

    fun devicesLimit(config: ConfigData): Int {
        if (isPlanExpired) {
            return config.planConfigForPlanTypeOrDefault(fallbackPlanType).devicesLimit
        } else {
            return devicesLimitOverride?.toInt()
                ?: config.planConfigForPlanTypeOrDefault(planType).devicesLimit
        }
    }

    fun placesLimit(config: ConfigData): Int? {
        return if (isPlanExpired) {
            config.planConfigForPlanTypeOrDefault(fallbackPlanType).placesLimit
        } else {
            placesLimitOverride?.toInt()
                ?: config.planConfigForPlanTypeOrDefault(planType).placesLimit
        }.takeIf { it >= 0 }
    }

    fun privacyZonesLimit(config: ConfigData): Int? {
        return if (isPlanExpired) {
            config.planConfigForPlanTypeOrDefault(fallbackPlanType).privacyZonesLimit
        } else {
            config.planConfigForPlanTypeOrDefault(planType).privacyZonesLimit
        }.takeIf { it >= 0 }
    }

    fun planConfig(config: ConfigData): PlanConfig {
        if (isPlanExpired) {
            return config.planConfigForPlanTypeOrDefault(fallbackPlanType)
        } else {
            return config.planConfigForPlanTypeOrDefault(planType)
        }
    }

    companion object {
        const val groupPublicSharingId = GroupOrSpaceId.groupPublicSharingId
        const val groupPersonalDevicesId = GroupOrSpaceId.groupPersonalDevicesId
        const val groupPrivateGroupId = GroupOrSpaceId.groupPrivateGroupId
    }
}

@Serializable
data class UserGroupWithMetadata constructor(
    val group: UserGroup,
    val memberCount: Int,
    val placesCount: Int,
    val privacyZonesCount: Int,
    val devicesPerMember: List<MemberRegisteredLocationCount>,
    val verification: VerificationResult,
) {
    val id: String get() = group.id
    val devicesCount: Int get() = devicesPerMember.firstOrNull { it.userId == appPrefs.userId }?.count ?: 0
    val isVerified get() = verification.isVerified(includeMemberVerification = group.enforceMemberAuthentication)
    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = group.nameOrNull
    val image: ImageModel get() = group.image

    fun arePlacesOverLimits(config: ConfigData): Boolean {
        val limit = group.placesLimit(config) ?: return true
        return placesCount > limit
    }

    fun arePrivacyZonesOverLimits(config: ConfigData): Boolean {
        val limit = group.privacyZonesLimit(config) ?: return true
        return privacyZonesCount > limit
    }

    fun isGroupOverLimits(config: ConfigData): Boolean {
        return memberCount > group.membersLimit(config) || arePlacesOverLimits(config) || devicesCount > group.devicesLimit(config)
    }
}

@Serializable
data class UserGroupData constructor(
    val name: String = "",
    val image: ImageModel = ImageModel(),
    val type: String = "",
    val enforceMemberAuthentication: Boolean = true
)


data class UserGroupWithMembers constructor(
    val group: UserGroup,
    val members: List<MemberModelWithLocations>
) {

    val isGroupVerified: Boolean
        get() {
            if (members.isEmpty()) {
                throw IllegalArgumentException("No members found in group")
            }

            val userId = appPrefs.userId

            if (userId.isBlank()) {
                throw IllegalArgumentException("Missing current user id from preferences")
            }

            val currentUser = memberByUserId(userId)?.member
                ?: throw IllegalArgumentException("Missing current user in group members")

            return currentUser.isVerified
        }

    val id: String get() = group.id
    val ownerUserId: String get() = group.ownerUserId
    val groupKeyVersion: Long get() = group.groupKeyVersion
    val data: CryptoResource<UserGroupData> get() = group.data
    val memberCount: Int get() = members.size

    val name: String get() = group.name
    val image: ImageModel get() = group.image

    val isCurrentUserOwner: Boolean get() = group.isCurrentUserOwner

    fun memberByUserId(userId: String): MemberModelWithLocations? {
        return members.firstOrNull { it.member.userId == userId }
    }

    val currentUserMemberWithLocations: MemberModelWithLocations? = memberByUserId(userId = appPrefs.userId)
    val currentUserMember: MemberModel? = currentUserMemberWithLocations?.member

    val currentDeviceLocation: MemberLocation?
        get() {
            return members.firstOrNull { it.member.userId == appPrefs.userId }?.currentDeviceLocation
        }
}

data class UserGroupWithMembersAndPlaces constructor(
    val groupWithMembers: UserGroupWithMembers,
    val places: List<GroupPlaceWithSettings>,
    val privacyZones: List<GroupPrivacyZone> = emptyList(),
) {
    val id: String get() = groupWithMembers.id
    val ownerUserId: String get() = groupWithMembers.ownerUserId
}
