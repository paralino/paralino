package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PersonalLocationHistoryCryptoKeyRaw(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val ownerUserId: String,
    val deviceId: String,
    val encryptedHistoryKey: String,
    val userKeyVersion: Long,
    val historyKeyVersion: String
) {
    companion object {
        fun from(map: Map<String, Any>): PersonalLocationHistoryCryptoKeyRaw {
            return PersonalLocationHistoryCryptoKeyRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).isoWithOffsetToInstant() ?: nowUTC(),
                ownerUserId = map["ownerUserId"] as String,
                deviceId = map["deviceId"] as String,
                encryptedHistoryKey = map["encryptedHistoryKey"] as String,
                userKeyVersion = (map["userKeyVersion"] as Number).toLong(),
                historyKeyVersion = map["historyKeyVersion"] as String
            )
        }
    }
}