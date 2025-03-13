package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupLocationHistoryCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val ownerUserId: String,
    val locationId: String,
    val deviceId: String,
    val memberKeyVersion: Long,
    val deviceKeyVersion: Long,
    val encryptedSenderKey: String,
) {
    companion object {
        fun from(map: Map<String, Any>): GroupLocationHistoryCryptoKeyRaw {
            return GroupLocationHistoryCryptoKeyRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                memberUserId = map["memberUserId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                locationId = map["locationId"] as String,
                deviceId = map["deviceId"] as String,
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                deviceKeyVersion = (map["deviceKeyVersion"] as Number).toLong(),
                encryptedSenderKey = map["encryptedSenderKey"] as String,
            )
        }
    }
}
