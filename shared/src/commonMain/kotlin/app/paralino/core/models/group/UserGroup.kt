package app.paralino.core.models.group

import app.paralino.core.config
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
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
    val planType: PlanType,
    val memberCount: Int,
    val verification: VerificationResult,
) {

    val isVerified get() = verification is VerificationResult.Verified
    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = data.data?.name
    val image: ImageModel get() = data.data?.image ?: ImageModel.emoji(Emojis.lock)

    companion object {
        const val personalTrackingId = "personalTracking"
        const val publicSharingId = "publicSharing"
    }

    val isCurrentUserOwner: Boolean get() = ownerUserId == config.userId

}

@Serializable
data class UserGroupData constructor(
    val name: String = "",
    val image: ImageModel = ImageModel(),
    val type: String = ""
)


data class UserGroupWithMembers constructor(
    val group: UserGroup,
    val members: List<MemberModelWithLocations>
) {

    val isGroupVerified get() = group.isVerified && memberByUserId(config.userId)!!.isVerified
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

    val currentDeviceLocation: MemberLocation? get() {
        return members.firstOrNull { it.member.userId == config.userId }?.currentDeviceLocation
    }

}