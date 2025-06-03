package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupLocationHistoryCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val groupId: String,
    val ownerUserId: String,
    val memberUserId: String,
    val deviceId: String,
    val memberKeyVersion: Long,
    val historyKeyVersion: String,
    val encryptedHistoryKey: String,
) {
    companion object {
        fun from(map: Map<String, Any>): GroupLocationHistoryCryptoKeyRaw {
            return GroupLocationHistoryCryptoKeyRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).isoWithOffsetToInstant() ?: nowUTC(),
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                memberUserId = map["memberUserId"] as String,
                deviceId = map["deviceId"] as String,
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                historyKeyVersion = map["historyKeyVersion"] as String,
                encryptedHistoryKey = map["encryptedHistoryKey"] as String,
            )
        }
    }
}
