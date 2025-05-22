package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PersonalLocationHistoryCryptoKeyRaw(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val encryptedHistoryKey: String,
    val userKeyVersion: Long,
    val historyKeyVersion: String
) {
    companion object {
        fun from(map: Map<String, Any>): PersonalLocationHistoryCryptoKeyRaw {
            return PersonalLocationHistoryCryptoKeyRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedHistoryKey = map["encryptedHistoryKey"] as String,
                userKeyVersion = (map["userKeyVersion"] as Number).toLong(),
                historyKeyVersion = map["historyKeyVersion"] as String
            )
        }
    }
}