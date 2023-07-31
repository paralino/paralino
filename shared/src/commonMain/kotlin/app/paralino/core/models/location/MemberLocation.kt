package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
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

    val isActive: Boolean
        get() {
            val now = nowUTC()
            val pinTime = locationData.data?.timeUTC?.fromUTCtoInstant() ?: return false
            val activeThreshold = 5.toDuration(DurationUnit.MINUTES)
            return pinTime.plus(activeThreshold) > now
        }

    val isEnded: Boolean
        get() {
            val now = nowUTC()
            val endTime = locationData.data?.endTimeUTC?.fromUTCtoInstant() ?: return false
            return endTime > now && isActive
        }
}