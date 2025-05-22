package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupLocationHistoryCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val memberUserId: String,
    val memberKeyVersion: Long,
    val historyKeyVersion: String,
    val encryptedHistoryKey: String,
) {
    companion object {
        fun from(map: Map<String, Any>): GroupLocationHistoryCryptoKeyRaw {
            return GroupLocationHistoryCryptoKeyRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                memberUserId = map["memberUserId"] as String,
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                historyKeyVersion = map["historyKeyVersion"] as String,
                encryptedHistoryKey = map["encryptedHistoryKey"] as String,
            )
        }
    }
}
