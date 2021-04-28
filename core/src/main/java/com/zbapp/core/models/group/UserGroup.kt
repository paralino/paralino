package com.zbapp.core.models.group

data class UserGroup constructor(
    val id: String,
    val name: String,
    val color: Int,
    val emoji: String,
    val userIds: List<String>
) {
    companion object {
        val personalTracking = UserGroup("personalTracking", "Personal", 0, "\uD83D\uDD12", emptyList())
        val publicSharing = UserGroup("publicSharing", "Public", 0, "\uD83C\uDFD9️", emptyList())
        fun empty() = UserGroup("", "", 0, "\uD83D\uDD12️", emptyList())
    }

    val isFromUser: Boolean get() = id != personalTracking.id && id != publicSharing.id

}
