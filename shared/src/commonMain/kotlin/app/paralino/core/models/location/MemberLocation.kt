package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.utils.Emojis
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

    @Contextual
    val deviceData: CryptoResource<MemberDeviceData>,
    @Contextual
    val locationData: CryptoResource<LocationDataModel>
) {
    val name: String? get() = deviceData.data?.deviceName

    val isActive: Boolean
        get() {
            val now = nowUTC()
            val pinTime = locationData.data?.timeUTC?.fromUTCtoInstant() ?: return false
            val activeThreshold = 30.toDuration(DurationUnit.MINUTES)
            return pinTime.plus(activeThreshold) > now
        }
}

@Serializable
data class MemberDeviceData constructor(
    val deviceName: String,
    val deviceType: String
)