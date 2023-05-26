package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDeviceCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val ownerUserId: String,
    val locationId: String,
    val memberKeyVersion: Long,
    val encryptedSenderKey: String,
) {
    companion object {
        fun from(map: Map<String, Any>): GroupDeviceCryptoKeyRaw {
            return GroupDeviceCryptoKeyRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                memberUserId = map["memberUserId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                locationId = map["locationId"] as String,
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                encryptedSenderKey = map["encryptedSenderKey"] as String,
            )
        }
    }
}
