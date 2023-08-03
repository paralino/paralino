package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class MemberLocation constructor(
    val id: String,
    val groupId: String,
    val memberId: String,
    val userId: String,
    val deviceId: String,
    val groupKeyVersion: Long,

    @Contextual
    val deviceData: CryptoResource<MemberDeviceData>,

    @Contextual
    val locationData: CryptoResource<LocationDataModel>,

    @Contextual
    val status: CryptoResource<MemberDeviceStatus>
) {
    val name: String? get() = deviceData.data?.deviceName ?: "---"

    val locationAwareStatusType: MemberDeviceStatusType?
        get() {
            return when {
                isEnded -> MemberDeviceStatusType.NOT_SHARING
                isExpired -> MemberDeviceStatusType.INACTIVE
                isIdleButStillActive -> MemberDeviceStatusType.ACTIVE
                isActiveButNotFresh -> null
                else -> status.data?.statusType ?: MemberDeviceStatusType.INACTIVE
            }
        }

    val isPinActive: Boolean
        get() = !isOlderThan(5.toDuration(DurationUnit.MINUTES))

    val isExpired: Boolean
        get() = isOlderThan(1.toDuration(DurationUnit.HOURS))

    val isActiveButNotFresh: Boolean
        get() {
            val activeStatus = status.data?.statusType == MemberDeviceStatusType.ACTIVE
                    && isOlderThan(2.toDuration(DurationUnit.MINUTES))
            val idleStatus = status.data?.statusType?.isIdling == true
                    && isOlderThan(10.toDuration(DurationUnit.MINUTES))
            return activeStatus || idleStatus
        }

    val isEnded: Boolean
        get() {
            val endTime = locationData.data?.endTimeUTC?.fromUTCtoInstant() ?: return false
            return endTime < nowUTC() && !isPinActive
        }

    // even if device status is not active (battery saving is on)
    // consider status as active if location is fairly fresh
    val isIdleButStillActive: Boolean
        get() = status.data?.statusType?.isIdling == true && !isOlderThan(20.toDuration(DurationUnit.SECONDS))

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = locationData.data?.timeUTC?.fromUTCtoInstant() ?: return false
        return pinTime.plus(duration) < nowUTC()
    }
}