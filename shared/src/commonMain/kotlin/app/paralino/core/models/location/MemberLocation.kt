package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    val name: String get() = nameOrNull ?: "---"
    val nameOrNull: String? get() = deviceData.data?.deviceName

    val canRequestLocation: Boolean get() {
        val adaptedStatus = locationAwareStatusType
        return (adaptedStatus != MemberDeviceStatusType.TOO_MANY_REQUESTS || isStatusOlderThan(1.minutes))
                && adaptedStatus != MemberDeviceStatusType.ACTIVE
                && adaptedStatus?.isDeviceSetupWrong != true
                && (isOlderThan(40.seconds) || !hasLocation)
                && status.data?.statusType != MemberDeviceStatusType.NOT_SHARING
    }

    val locationAwareStatusType: MemberDeviceStatusType?
        get() {
            return when {
                isEnded -> MemberDeviceStatusType.NOT_SHARING
                status.data?.statusType == MemberDeviceStatusType.NOT_SHARING -> MemberDeviceStatusType.NOT_SHARING
                isExpired -> {
                    if (status.data?.statusType?.isDeviceSetupWrong == true && !isStatusOlderThan(1.hours)) {
                        status.data?.statusType ?: MemberDeviceStatusType.INACTIVE
                    } else {
                        MemberDeviceStatusType.INACTIVE
                    }
                }
                isActiveEvenIfIdle -> MemberDeviceStatusType.ACTIVE
                isActiveButNotFresh && !isStatusOlderThan(20.seconds) && status.data?.statusType == MemberDeviceStatusType.ACTIVE -> MemberDeviceStatusType.ACTIVE_NO_LOCATION
                isActiveButNotFresh && !isStatusOlderThan(10.minutes) && status.data?.statusType?.isIdlingOrActive == true -> MemberDeviceStatusType.IDLE
                isActiveButNotFresh -> null
                (!hasLocation || isOlderThan(2.minutes)) && !isStatusOlderThan(20.seconds) && status.data?.statusType == MemberDeviceStatusType.ACTIVE -> MemberDeviceStatusType.ACTIVE_NO_LOCATION
                !hasLocation && isStatusOlderThan(20.seconds) && status.data?.statusType?.isIdlingOrActive == true -> MemberDeviceStatusType.IDLE_NO_LOCATION
                !hasLocation && status.data?.statusType == MemberDeviceStatusType.IDLE -> MemberDeviceStatusType.IDLE_NO_LOCATION
                else -> status.data?.statusType ?: MemberDeviceStatusType.INACTIVE
            }
        }

    val hasLocation: Boolean get() = locationData.data != null
    val hasStatus: Boolean get() = status.data != null

    val isPinActive: Boolean
        get() = !isOlderThan(5.minutes) && !isEnded

    val isExpired: Boolean
        get() = isOlderThan(1.hours) && isStatusOlderThan(2.hours) || (!hasLocation && isStatusOlderThan(2.hours))

    val isActiveButNotFresh: Boolean
        get() {
            val activeStatus = status.data?.statusType == MemberDeviceStatusType.ACTIVE
                    && isOlderThan(30.seconds)
            val idleStatus = status.data?.statusType?.isIdling == true
                    && isOlderThan(10.minutes)
            return activeStatus || idleStatus
        }

    val isEnded: Boolean
        get() {
            if (status.data?.statusType == MemberDeviceStatusType.NOT_SHARING) return true
            val endTime = locationData.data?.endTimeUTC?.fromUTCtoInstant() ?: return false
            return endTime < nowUTC() && isOlderThan(5.minutes)
        }

    val canAutomaticallyClearLocation: Boolean
        get() { return hasLocation && isOlderThan(7.days) && status.data?.statusType?.isInBlockedState != false }

    val isLocationAvailableButOld: Boolean
        get() { return hasLocation && (isEnded || isOlderThan(12.hours)) }

    // even if device status is not active (battery saving is on)
    // consider status as active if location is fairly fresh
    val isActiveEvenIfIdle: Boolean
        get() {
            if (!hasLocation) return false
            return status.data?.statusType?.isIdling == true && !isOlderThan(20.seconds)
        }

    private fun isOlderThan(duration: Duration): Boolean {
        return locationData.data?.isOlderThan(duration) ?: return false
    }

    fun isStatusOlderThan(duration: Duration): Boolean {
        return status.data?.isOlderThan(duration) ?: return false
    }
}