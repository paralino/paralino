package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isInPast
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class PushNotificationRaw constructor(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val expiresAt: Instant?,
    val deviceId: String?,
    val eventId: String?,
    val type: String,
    val importance: String,
    val content: String?,
) {

    val isExpired: Boolean
        get() {
            return if (expiresAt != null) {
                expiresAt.isInPast()
            } else {
                createdAt.isOlderThan(1.days)
            }
        }

    val pushType: PushNotificationType? = PushNotificationType.fromKey(key = type)
    val pushImportance: PushNotificationImportance? =
        PushNotificationImportance.fromKey(key = importance)

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): PushNotificationRaw {
            return PushNotificationRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        nowUTC()
                    }
                },
                expiresAt = (map["expiresAt"] as? String)?.let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        nowUTC()
                    }
                },
                deviceId = map["deviceId"] as? String?,
                eventId = map["eventId"] as? String?,
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
    LOW_BATTERY_ALERT("low_battery_alert"),

    GROUP_PLACE_ALERT("place_activity"),
    GROUP_PLACE_CREATED("place_created"),
    GROUP_PLACE_UPDATED("place_updated"),
    GROUP_PLACE_DELETED("place_deleted"),

    PERSONAL_PLACE_ALERT("personal_place_activity"),
    PERSONAL_PLACE_CREATED("personal_place_created"),
    PERSONAL_PLACE_UPDATED("personal_place_updated"),
    PERSONAL_PLACE_DELETED("personal_place_deleted"),

    GROUP_MEMBER_ADDED("group_member_added"),
    GROUP_MEMBER_DELETED("group_member_deleted"),
    LOCATION_STARTED("location_sharing_start"),
    LOCATION_REQUEST("location_request"),
    LOCATION_FOCUS("location_focus");

    companion object {
        fun fromKey(key: String?): PushNotificationType? {
            return entries.firstOrNull { it.key == key }
        }
    }
}