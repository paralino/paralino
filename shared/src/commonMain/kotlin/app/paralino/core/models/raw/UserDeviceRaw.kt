package app.paralino.core.models.raw

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class UserDeviceRaw constructor(
    @SerialName("\$id")
    val id: String,
    val deviceType: String,
    val pushToken: String?
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): UserDeviceRaw {
            return UserDeviceRaw(
                id = map["\$id"] as String,
                deviceType = map["deviceType"] as String,
                pushToken = map["pushToken"] as? String?,
            )
        }
    }
}
