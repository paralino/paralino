package app.paralino.core.models.raw

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class UserDeviceRaw constructor(
    @SerialName("\$id")
    val id: String,
    val pushTargetId: String?,
    val sessionId: String?,
    val deviceType: String,
    val ownerUserId: String,
    val deviceId: String,
    val locationPushToken: String?,
    val encryptedSenderKey: String?,
    val userKeyVersion: Long?,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any?>): UserDeviceRaw {
            return UserDeviceRaw(
                id = map["\$id"] as String,
                pushTargetId = map["pushTargetId"] as? String,
                sessionId = map["sessionId"] as? String,
                deviceType = map["deviceType"] as String,
                ownerUserId = map["ownerUserId"] as String,
                deviceId = map["deviceId"] as String,
                locationPushToken = map["locationPushToken"] as? String,
                encryptedSenderKey = map["encryptedSenderKey"] as? String,
                userKeyVersion = (map["userKeyVersion"] as? Number?)?.toLong()
            )
        }
    }
}
