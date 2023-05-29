package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberLocationRaw constructor(
    @SerialName("\$id")
    val id: String,
    val deviceId: String,

    val groupKeyVersion: Long,
    val deviceKeyVersion: Long,
    val deviceData: String? = null,
    val locationData: String? = null,
) {

    companion object {
        fun from(map: Map<String, Any>): MemberLocationRaw {
            return MemberLocationRaw(
                id = map["\$id"] as String,
                deviceId = map["deviceId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                deviceKeyVersion = (map["deviceKeyVersion"] as Number).toLong(),
                deviceData = map["deviceData"] as? String?,
                locationData = map["locationData"] as? String?,
            )
        }
    }
}
