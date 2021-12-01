package com.zbapp.core.models.group

import com.zbapp.core.config
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val ownerId: String = "",
    val type: String = "",
    val name: String = "",
    val emoji: String = "",
    val members: List<MemberModel> = emptyList()
) {
    companion object {
        val personalTracking = UserGroup("personalTracking", "", Constants.collectionGroupPrivate, "Personal", "\uD83D\uDD12", emptyList())
        val publicSharing = UserGroup("publicSharing", "", Constants.collectionGroupPublic, "Public", "\uD83C\uDFD9️", emptyList())
        fun empty(type: String) = UserGroup("", type, "","\uD83D\uDD12️", "", emptyList())
        fun new(icon: String, name: String) = UserGroup("", config.userId, Constants.collectionGroupUser, name, icon, emptyList())
    }

    val isFromUser: Boolean get() = type == Constants.collectionGroupUser

}
