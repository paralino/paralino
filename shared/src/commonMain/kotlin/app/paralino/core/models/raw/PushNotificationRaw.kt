package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Serializable
data class PushNotificationRaw constructor(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val source: String?,
    val deviceId: String?,
    val type: String,
    val importance: String,
    val content: String?,
) {

    // TODO adjust time check
    val isExpired: Boolean get() = isOlderThan(1.days)
    fun isOlderThan(duration: Duration): Boolean = createdAt.plus(duration) < nowUTC()

    val pushType: PushNotificationType? = PushNotificationType.fromKey(key = type)
    val pushImportance: PushNotificationImportance? = PushNotificationImportance.fromKey(key = type)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): PushNotificationRaw {
            return PushNotificationRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).let {
                    Instant.parse(it)
                },
                source = map["source"] as? String?,
                deviceId = map["deviceId"] as? String?,
                type = map["type"] as String,
                importance = map["importance"] as String,
                content = map["content"] as? String?,
            )
        }
    }
}

enum class PushNotificationImportance constructor(val key: String) {
    NORMAL("normal"),
    MEDIUM("medium"),
    HIGH("high");

    companion object {
        fun fromKey(key: String?): PushNotificationImportance? {
            return entries.firstOrNull { it.key == key }
        }
    }
}

enum class PushNotificationType constructor(val key: String) {
    PLACE_ALERT("place_activity"),
    PLACE_CREATED("place_created"),
    PLACE_UPDATED("place_updated"),
    PLACE_DELETED("place_deleted"),
    GROUP_MEMBER_ADDED("group_member_added"),
    GROUP_MEMBER_DELETED("group_member_deleted"),
    LOCATION_REQUEST("location_request"),
    LOCATION_FOCUS("location_focus");

    companion object {
        fun fromKey(key: String?): PushNotificationType? {
            return entries.firstOrNull { it.key == key }
        }
    }
}