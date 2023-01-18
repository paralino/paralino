package app.paralino.core.models.group

import app.paralino.core.config
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.utils.Emojis
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerUserId: String,
    val keyVersion: Long,
    val shouldRotateKeys: Boolean,
    @Contextual
    val data: CryptoResource<UserGroupData>,
    val memberCount: Int
) {

    val name: String get() = data.data?.name ?: "---"
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

    val id: String get() = group.id
    val ownerUserId: String get() = group.ownerUserId
    val keyVersion: Long get() = group.keyVersion
    val shouldRotateKeys: Boolean get() = group.shouldRotateKeys
    val data: CryptoResource<UserGroupData> get() = group.data
    val memberCount: Int get() = members.size

    val name: String get() = group.name
    val image: ImageModel get() = group.image

    val isCurrentUserOwner: Boolean get() = group.isCurrentUserOwner

    fun memberByUserId(userId: String): MemberModel {
        return members.first { it.member.userId == userId }.member
    }

}