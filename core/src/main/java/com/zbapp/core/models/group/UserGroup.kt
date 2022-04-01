package com.zbapp.core.models.group

import com.zbapp.core.config
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: UserGroupData = UserGroupData(),
    val members: List<MemberModel> = emptyList()
) {

    val name: String get() = data.name
    val emoji: String get() = data.emoji
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
    val emoji: String = "",
    val type: String = ""
) {

}
