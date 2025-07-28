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
    val userKeyVersion: Long?,
    val private: Boolean,
    val isInPersonalSpace: Boolean?,
    val data: String = "",
    val eventKeys: List<String>
) {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): GroupPlaceRaw {
            return GroupPlaceRaw(
                id = map["\$id"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                groupKeyVersion = (map["groupKeyVersion"] as Number).toLong(),
                userKeyVersion = (map["userKeyVersion"] as? Number)?.toLong(),
                private = map["private"] as Boolean,
                isInPersonalSpace = map["isInPersonalSpace"] as? Boolean,
                data = map["data"] as String,
                eventKeys = map["eventKeys"] as List<String>
            )
        }
    }
}

@Serializable
data class GroupPlaceWithSettingsRaw constructor(
    val place: GroupPlaceRaw,
    val settings: GroupPlaceSettingRaw?
)