package com.zbapp.core.models.location

data class LocationSharingContract constructor(
    val groupId: String,
    val endTimeUTC: String?
) {

    val isToGroup: Boolean = groupId.startsWith("group:")
    val id: String = groupId.replace("group:" ,"").replace("user:", "")

    companion object {
        fun toUser(userId: String, endTimeUTC: String?) = LocationSharingContract("user:$userId", endTimeUTC)
        fun toGroup(groupId: String, endTimeUTC: String?) = LocationSharingContract("group:$groupId", endTimeUTC)
    }

}
