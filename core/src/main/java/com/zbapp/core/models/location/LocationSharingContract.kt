package com.zbapp.core.models.location

import com.zbapp.core.repo.LocationSharingManager
import kotlinx.datetime.*
import java.time.LocalDateTime

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
        fun forDuration(durationMillis: Long): String {
            val now = Clock.System.now()
            return now.plus(durationMillis, DateTimeUnit.MILLISECOND).toLocalDateTime(TimeZone.UTC).toString()
        }
    }

}
