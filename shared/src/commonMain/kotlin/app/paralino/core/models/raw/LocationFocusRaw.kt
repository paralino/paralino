package app.paralino.core.models.raw

import app.paralino.core.notifications.LocationRequestNotificationPayload
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.toJson
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlin.time.Duration.Companion.minutes

data class LocationFocusRaw(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    @SerialName("\$updatedAt")
    val updatedAt: Instant,

    val ownerUserId: String,
    val groupId: String,
) {
    fun asValidPushNotification(): PushNotificationRaw {
        return PushNotificationRaw(
            id = id,
            createdAt = createdAt,
            expiresAt = updatedAt.plus(5.minutes),
            deviceId = null,
            eventId = "$groupId$ownerUserId",
            type = PushNotificationType.LOCATION_FOCUS.key,
            importance = PushNotificationImportance.NORMAL.key,
            content = LocationRequestNotificationPayload(
                groupId = groupId,
                memberUserId = ownerUserId
            ).toJson()
        )
    }

    companion object {
        fun fromDocument(map: Map<String, Any>): LocationFocusRaw {
            return LocationFocusRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).isoWithOffsetToInstant() ?: nowUTC(),
                updatedAt = (map["\$updatedAt"] as String).isoWithOffsetToInstant() ?: nowUTC(),
                ownerUserId = map.get("ownerUserId") as String,
                groupId = map.get("groupId") as String,
            )
        }
    }
}
