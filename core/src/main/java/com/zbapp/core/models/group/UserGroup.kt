package com.zbapp.core.models.group

import com.zbapp.core.utils.Constants
import kotlinx.serialization.Serializable

@Serializable
data class UserGroup constructor(
    val id: String,
    val type: String = "",
    val name: String,
    val color: Int,
    val emoji: String,
    val ownerId: String,
    val userIds: List<String>,
    val isVisibleToEveryone: Boolean = false
) {
    companion object {
        val personalTracking = UserGroup("personalTracking", Constants.collectionGroupPrivate, "Personal", 0, "\uD83D\uDD12", "", emptyList())
        val publicSharing = UserGroup("publicSharing", Constants.collectionGroupPublic, "Public", 0, "\uD83C\uDFD9️", "", emptyList())
        fun empty(type: String) = UserGroup("", type, "", 0, "\uD83D\uDD12️", "", emptyList())
        fun new(icon: String, name: String) = UserGroup("", Constants.collectionGroupUser, name, 0, icon, "", emptyList())
    }

    val isFromUser: Boolean get() = type == Constants.collectionGroupUser

}
