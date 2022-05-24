package com.zbapp.core.models.location

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.utils.SharingDurationPreset
import com.zbapp.core.utils.extensions.fromUTCtoInstant
import com.zbapp.core.utils.extensions.nowUTC
import kotlinx.datetime.*

data class LocationSharingContract constructor(
    val groupId: String,
    val endTimeUTC: String?,
    val shareBattery: Boolean,
    val shareSpeed: Boolean,
    val shareElevation: Boolean,
    val approximateLocationInMeters: Int?
) {

    val isAlwaysOn: Boolean get() = endTimeUTC == null
    val isLongTerm: Boolean get() = isLongTerm(endsInMillis)

    val endsInMillis: Long get() {
        if (isAlwaysOn) return 0
        val now = nowUTC()
        val end = endTimeUTC!!.fromUTCtoInstant() ?: return 0
        val diff = end - now
        return diff.inWholeMilliseconds
    }

    val endTimeMillisUTC: Long? get() {
        if (isAlwaysOn) return null
        return endTimeUTC!!.fromUTCtoInstant()?.toEpochMilliseconds()
    }

    fun asValidContract(): LocationSharingContract? {
        if(isAlwaysOn) {
            // invalid public sharing contract
            if(this.groupId == UserGroup.publicSharingId) return null
            // endless sharing for other groups
            else return this
        } else {
            val date = endTimeUTC!!.fromUTCtoInstant() ?: return null
            val now = nowUTC()
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
        fun isLongTerm(durationMillis: Long): Boolean {
            return durationMillis == 0L || durationMillis >= SharingDurationPreset.backgroundAccessThreshold
        }

        fun forDuration(durationMillis: Long): String? {
            if(durationMillis == 0L) return null
            return nowUTC()
                .plus(durationMillis, DateTimeUnit.MILLISECOND)
                .toLocalDateTime(TimeZone.UTC)
                .toString()
        }
    }

}
