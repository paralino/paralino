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
data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,
    val publicKey: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,
    @Contextual
    val memberData: CryptoResource<MemberModelData>,
) {

    val name: String get() = memberData.data?.name ?: "---"
    val image: ImageModel? get() = memberData.data?.image ?: ImageModel.emoji(Emojis.lock)

}

@Serializable
data class MemberModelWithLocations(
    val member: MemberModel,
    val locations: List<MemberLocation>
) {
    val isActive: Boolean
        get() {
            val now = nowUTC()
            val pinTime =
                locations.firstNotNullOfOrNull { it.locationData?.data?.timeUTC?.fromUTCtoInstant() }
                    ?: return false
            val activeThreshold = 30.toDuration(DurationUnit.MINUTES)
            return pinTime.plus(activeThreshold) > now
        }

    val hasLocation: Boolean
        get() = locations.firstNotNullOfOrNull { it.locationData.data } != null
}

@Serializable
data class MemberModelData constructor(
    val name: String,
    val image: ImageModel? = null
)
