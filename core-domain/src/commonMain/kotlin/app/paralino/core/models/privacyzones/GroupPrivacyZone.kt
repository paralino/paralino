package app.paralino.core.models.privacyzones

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GroupPrivacyZone constructor(
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val userKeyVersion: Long,

    @Contextual
    val data: CryptoResource<PrivacyZoneData>,
) {
    val nameOrNull: String? get() = data.data?.name
}

@Serializable
data class PrivacyZoneData constructor(
    val name: String,
    val image: ImageModel,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
    val expiresAt: String,
    val notifyOthersInZone: Boolean = false,
    val inZoneMessage: String? = null,
) {
    fun isExpired(now: Instant = nowUTC()): Boolean {
        val expires = expiresAt.isoWithOffsetToInstant() ?: return false
        return expires <= now
    }
}
