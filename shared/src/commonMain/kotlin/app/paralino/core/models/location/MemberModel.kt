package app.paralino.core.models.location

import app.paralino.core.config
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.GroupSettings
import app.paralino.core.utils.Emojis
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MemberModel constructor(
    val id: String,
    val groupId: String,
    val userId: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val verification: String,
    @Contextual
    val memberData: CryptoResource<MemberModelData>,
    val settings: GroupSettings?,
    val verificationResult: VerificationResult
) {

    val name: String get() = memberData.data?.name ?: "---"
    val image: ImageModel? get() = memberData.data?.image ?: ImageModel.emoji(Emojis.lock)
    val isVerified: Boolean get() = verificationResult == VerificationResult.VERIFIED

}

@Serializable
data class MemberModelWithLocations(
    val member: MemberModel,
    val locations: List<MemberLocation>
) {
    val isActive: Boolean get() = locations.any { it.isActive }
    val hasLocation: Boolean get() = locations.firstNotNullOfOrNull { it.locationData.data } != null
    val currentDeviceLocation: MemberLocation? get() {
        return locations.firstOrNull { it.deviceId == config.deviceId && it.userId == config.userId }
    }
}

@Serializable
data class MemberModelData constructor(
    val name: String,
    val image: ImageModel? = null
)
