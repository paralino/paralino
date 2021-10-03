package com.zbapp.core.models.location

data class LocationSharingContract constructor(
    val groupId: String,
    val endTimeUTC: String?
) {

    val isToGroup: Boolean = groupId.startsWith(groupPrefix)
    val objectId: String = groupId.replace(groupPrefix ,"").replace(userPrefix, "")

    companion object {
        const val groupPrefix = "groupID:"
        const val userPrefix = "userID:"
        fun toUser(userId: String, endTimeUTC: String?) = LocationSharingContract("$userPrefix$userId", endTimeUTC)
        fun toGroup(groupId: String, endTimeUTC: String?) = LocationSharingContract("$groupPrefix$groupId", endTimeUTC)
    }

}
