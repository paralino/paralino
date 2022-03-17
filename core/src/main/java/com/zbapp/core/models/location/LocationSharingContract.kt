package com.zbapp.core.models.location

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.utils.SharingDurationPreset
import kotlinx.datetime.*

data class LocationSharingContract constructor(
    val groupId: String,
    val endTimeUTC: String?
) {

    val objectId: String = groupId.replace(groupPrefix ,"").replace(userPrefix, "")

    val isToGroup: Boolean = groupId.startsWith(groupPrefix)
    val isAlwaysOn: Boolean get() = endTimeUTC == null
    val isLongTerm: Boolean get() = isAlwaysOn || endsInMillis >= SharingDurationPreset.backgroundAccessThreshold

    val endsInMillis: Long get() {
        if (isAlwaysOn) return 0
        val now = Clock.System.now()
        val end = LocalDateTime.parse(endTimeUTC!!).toInstant(TimeZone.UTC)
        val diff = end - now
        return diff.inWholeMilliseconds
    }

    val endTimeMillisUTC: Long? get() {
        if (isAlwaysOn) return null
        return LocalDateTime.parse(endTimeUTC!!).toInstant(TimeZone.UTC).toEpochMilliseconds()
    }

    fun asValidContract(): LocationSharingContract? {
        if(isAlwaysOn) {
            // invalid public sharing contract
            if(this.groupId == UserGroup.publicSharingId) return null
            // endless sharing for other groups
            else return this
        } else {
            val date = LocalDateTime.parse(endTimeUTC!!).toInstant(TimeZone.UTC)
            val now = Clock.System.now()
            if(date <= now) {
                // expired
                return null
            } else {
                // limit public sharing if needed
                val maxPublic = now.plus(SharingDurationPreset.publicSharingMaxDurationMillis, DateTimeUnit.MILLISECOND)
                if(this.groupId == UserGroup.publicSharingId && date > maxPublic)
                    return this.copy(endTimeUTC = maxPublic.toLocalDateTime(TimeZone.UTC).toString())

                return this
            }
        }
    }

    companion object {
        const val groupPrefix = "groupID:"
        const val userPrefix = "userID:"
        fun toUser(userId: String, endTimeUTC: String?) = LocationSharingContract("$userPrefix$userId", endTimeUTC)
        fun toGroup(groupId: String, endTimeUTC: String?) = LocationSharingContract("$groupPrefix$groupId", endTimeUTC)

        fun forDuration(durationMillis: Long): String? {
            if(durationMillis == 0L) return null
            val now = Clock.System.now()
            return now
                .plus(durationMillis, DateTimeUnit.MILLISECOND)
                .toLocalDateTime(TimeZone.UTC)
                .toString()
        }
    }

}
