package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val private: Boolean,
    val data: String = ""
) {

    fun toPostJson(): Map<String, Any> {
        return buildMap {
            put("private", private)
            put("data", data)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupPlaceRaw {
            return GroupPlaceRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                ownerPublicKey = map["ownerPublicKey"] as String,
                keyVersion = map["keyVersion"] as Long,
                private = map["private"] as Boolean,
                data = map["data"] as String,
            )
        }
    }
}

@Serializable
data class GroupPlacesWithSettingsRaw constructor(
    val place: GroupPlaceRaw,
    val settings: GroupPlaceSettingRaw?
)