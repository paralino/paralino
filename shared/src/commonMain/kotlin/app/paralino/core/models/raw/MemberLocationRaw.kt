package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberLocationRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val memberId: String,
    val userId: String,
    val deviceId: String,

    val deviceData: String? = null,
    val locationData: String? = null,
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): MemberLocationRaw {
            return MemberLocationRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                memberId = map["memberId"] as String,
                userId = map["userId"] as String,
                deviceId = map["deviceId"] as String,
                deviceData = map["deviceData"] as? String?,
                locationData = map["locationData"] as? String?,
            )
        }
    }
}
