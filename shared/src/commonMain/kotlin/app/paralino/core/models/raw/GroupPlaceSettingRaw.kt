package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceSettingRaw(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val placeId: String,
    val ownerUserId: String,
) {

    fun toPostJson(): Map<String, Any> {
        return buildMap {
            put("groupId", groupId)
            put("placeId", placeId)
            put("ownerUserId", ownerUserId)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupPlaceSettingRaw {
            return GroupPlaceSettingRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                placeId = map["placeId"] as String,
                ownerUserId = map["ownerUserId"] as String
            )
        }
    }
}
