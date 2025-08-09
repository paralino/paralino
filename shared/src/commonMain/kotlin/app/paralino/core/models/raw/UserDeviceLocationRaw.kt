package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDeviceLocationRaw constructor(
    @SerialName("\$id")
    val id: String,
    val locationData: String?,
    val status: String?,
    val locationHistory: String?
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any?>): UserDeviceLocationRaw {
            return UserDeviceLocationRaw(
                id = map["\$id"] as String,
                locationData = map["locationData"] as? String,
                status = map["status"] as? String,
                locationHistory = map["locationHistory"] as? String
            )
        }
    }
}
