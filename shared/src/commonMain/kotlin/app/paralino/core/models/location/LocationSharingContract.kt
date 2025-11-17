package app.paralino.core.models.location

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.utils.SharingDurationPreset
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

data class LocationSharingContract constructor(
    val groupOrSpaceId: GroupOrSpaceId,
    val endTimeUTCPreference: String?,
    val endTimeUTCOverride: String?,
    val shareBattery: Boolean,
    val shareSpeed: Boolean,
    val shareElevation: Boolean,
    val shareNetwork: Boolean,
    val shareDeviceStatus: Boolean,
    val approximateLocationInMeters: Int?,
    val saveToHistory: Boolean,
    val isEnabled: Boolean,
) {

    val endTimeUTC: String? = endTimeUTCOverride ?: endTimeUTCPreference
    val isAlwaysOn: Boolean get() = endTimeUTC == null
    val isLongTerm: Boolean get() = isLongTerm(endsInMillis)

    private val endsInMillis: Long get() {
        if (isAlwaysOn) return -1
        val now = nowUTC()
        val end = endTimeUTC!!.fromUTCtoInstant() ?: return -1
        val diff = end - now
        return diff.inWholeMilliseconds
    }

    fun asValidContract(newEndTimeUTCOverride: String? = endTimeUTCOverride): LocationSharingContract? {
        if (!isEnabled) {
            return null
        } else if(isAlwaysOn) {
            // invalid public sharing contract
            if(this.groupOrSpaceId.isPublicSharing) return null
            // endless sharing for other groups
            else return this.copy(endTimeUTCOverride = newEndTimeUTCOverride)
        } else {
            val date = endTimeUTC!!.fromUTCtoInstant() ?: return null
            val now = nowUTC()
            if(date <= now) {
                // expired
                return null
            } else if (this.groupOrSpaceId.isPublicSharing) {
                // limit public sharing if needed
                val maxPublic = now.plus(SharingDurationPreset.publicSharingMaxDurationMillis, DateTimeUnit.MILLISECOND)
                if(date > maxPublic) {
                    return this.copy(endTimeUTCPreference = maxPublic.toLocalDateTime(TimeZone.UTC).toString())
                } else {
                    return this.copy(endTimeUTCOverride = newEndTimeUTCOverride)
                }
            } else {
                return this.copy(endTimeUTCOverride = newEndTimeUTCOverride)
            }
        }
    }

    companion object {
        fun isLongTerm(durationMillis: Long): Boolean {
            return durationMillis <= 0L || durationMillis >= SharingDurationPreset.backgroundAccessThreshold
        }

        fun forDuration(durationMillis: Long): String? {
            if(durationMillis <= 0L) return null
            return nowUTC()
                .plus(durationMillis, DateTimeUnit.MILLISECOND)
                .toLocalDateTime(TimeZone.UTC)
                .toString()
        }
    }
}
