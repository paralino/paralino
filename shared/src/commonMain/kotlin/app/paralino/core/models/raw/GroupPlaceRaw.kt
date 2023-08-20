package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupPlaceRaw constructor(
    @SerialName("\$id")
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val groupKeyVersion: Long,
    val private: Boolean,
    val data: String = ""
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupPlaceRaw {
            return GroupPlaceRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
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