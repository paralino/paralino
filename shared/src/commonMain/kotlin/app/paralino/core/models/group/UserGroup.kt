package app.paralino.core.models.group

import app.paralino.core.config
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.places.GroupPlaceWithSettings
import app.paralino.core.utils.Emojis
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    @Contextual
    val data: CryptoResource<UserGroupData>,
    val serverIntegrity: String,
    val planType: String,
    val schemeVersion: String,
    val membersLimitOverride: Long?,
    val devicesLimitOverride: Long?,
    val placesLimitOverride: Long?
) {

    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = data.data?.name
    val image: ImageModel get() = data.data?.image ?: ImageModel.emoji(Emojis.lock)
    val isCurrentUserOwner: Boolean get() = ownerUserId == config.userId
    val enforceMemberAuthentication: Boolean = data.data?.enforceMemberAuthentication != false

    companion object {
        const val groupPublicSharingId = "publicSharing"
        const val groupPrivateGroupId = "privateGroup"
        const val groupPersonalDevicesId = "personalDevices"
    }
}

@Serializable
data class UserGroupWithMetadata constructor(
    val group: UserGroup,
    val memberCount: Int,
    val verification: VerificationResult,
) {
    val id: String get() = group.id
    val isVerified get() = verification.isVerified(includeMemberVerification = group.enforceMemberAuthentication)
    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = group.nameOrNull
    val image: ImageModel get() = group.image
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

    val isGroupVerified get() = memberByUserId(config.userId)!!.isVerified
    val id: String get() = group.id
    val ownerUserId: String get() = group.ownerUserId
    val groupKeyVersion: Long get() = group.groupKeyVersion
    val data: CryptoResource<UserGroupData> get() = group.data
    val memberCount: Int get() = members.size

    val name: String get() = group.name
    val image: ImageModel get() = group.image

    val isCurrentUserOwner: Boolean get() = group.isCurrentUserOwner

    fun memberByUserId(userId: String): MemberModel? {
        return members.firstOrNull { it.member.userId == userId }?.member
    }

    val currentDeviceLocation: MemberLocation?
        get() {
            return members.firstOrNull { it.member.userId == config.userId }?.currentDeviceLocation
        }
}

data class UserGroupWithMembersAndPlaces constructor(
    val groupWithMembers: UserGroupWithMembers,
    val places: List<GroupPlaceWithSettings>
) {
    val id: String get() = groupWithMembers.id
    val ownerUserId: String get() = groupWithMembers.ownerUserId
}
