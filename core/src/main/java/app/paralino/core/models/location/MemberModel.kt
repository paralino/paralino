package app.paralino.core.models.location

import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,
    val publicKey: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: MemberModelData,
    val mutableData: LocationDataModel? = null
) {

    val name: String get() = data.name
    val image: ImageModel? get() = data.image

    val isActive: Boolean
        get() {
            val now = nowUTC()
            val pinTime = mutableData?.timeUTC?.fromUTCtoInstant() ?: return false
            val activeThreshold = 30.toDuration(DurationUnit.MINUTES)
            return pinTime.plus(activeThreshold) > now
        }

    companion object {
        fun fromSystem(
            userId: String,
            name: String,
            image: ImageModel?,
            model: LocationDataModel
        ): MemberModel {
            return MemberModel(
                id = "",
                groupId = "",
                userId = userId,
                publicKey = "",
                groupKeyVersion = 0,
                memberKeyVersion = 0,
                shouldRotateKeys = false,
                data = MemberModelData(name, image),
                mutableData = model
            )
        }
    }

}

@Serializable
data class MemberModelData constructor(
    val name: String,
    val image: ImageModel? = null
)

@Serializable
data class LocationDataModel constructor(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    val timeUTC: String? = null,
    val batteryLevel: Int? = null,
    val speed: Float? = null,
    val altitude: Float? = null
) {
    val shouldShowApproximateCircle get() = accuracy > 10f
}
