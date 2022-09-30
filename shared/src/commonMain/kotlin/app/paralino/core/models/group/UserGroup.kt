package app.paralino.core.models.group

import app.paralino.core.config
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: UserGroupData = UserGroupData(),
    val memberCount: Int
) {

    val name: String get() = data.name
    val image: ImageModel get() = data.image
    val type: String get() = data.type

    companion object {
        const val personalTrackingId = "personalTracking"
        const val publicSharingId = "publicSharing"
    }

    val isFromUser: Boolean get() = type == Constants.collectionGroupUser
    val isCurrentUserOwner: Boolean get() = ownerUserId == config.userId

}

@Serializable
data class UserGroupData constructor(
    val name: String = "",
    val image: ImageModel = ImageModel(),
    val type: String = ""
) {

}

data class UserGroupWithMembers(
    val group: UserGroup,
    val members: List<MemberModel>
) {

    val id: String get() = group.id
    val ownerUserId: String get() = group.ownerUserId
    val ownerPublicKey: String get() = group.ownerPublicKey
    val keyVersion: Long get() = group.keyVersion
    val shouldRotateKeys: Boolean get() = group.shouldRotateKeys
    val data: UserGroupData get() = group.data
    val memberCount: Int get() = members.size

    val name: String get() = data.name
    val image: ImageModel get() = data.image
    val type: String get() = data.type

    val isCurrentUserOwner: Boolean get() = group.isCurrentUserOwner

    fun memberByUserId(userId: String): MemberModel {
        return members.first { it.userId == userId }
    }

}