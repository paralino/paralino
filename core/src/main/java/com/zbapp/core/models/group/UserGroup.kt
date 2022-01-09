package com.zbapp.core.models.group

import com.zbapp.core.config
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerId: String = "",
    val data: UserGroupData = UserGroupData(),
    val members: List<MemberModel> = emptyList()
) {

    val name: String get() = data.name
    val emoji: String get() = data.emoji
    val type: String get() = data.type

    companion object {
        val personalTracking = UserGroup("personalTracking", "", UserGroupData("Personal", "\uD83D\uDD12", Constants.collectionGroupPrivate), emptyList())
        val publicSharing = UserGroup("publicSharing", "", UserGroupData("Public", "\uD83C\uDFD9️", Constants.collectionGroupPublic), emptyList())
        fun new(icon: String, name: String) = UserGroup(
            id = "",
            ownerId = config.userId,
            data = UserGroupData(name, icon, Constants.collectionGroupUser),
            members = emptyList()
        )
    }

    val isFromUser: Boolean get() = type == Constants.collectionGroupUser

}

@Serializable
data class UserGroupData constructor(
    val name: String = "",
    val emoji: String = "",
    val type: String = ""
) {

}