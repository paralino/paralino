package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

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

    companion object {

        const val pushTypePlaceAlert: String = "place_activity"
        const val pushTypeGroupMemberAdd = "group_member_added"
        const val pushTypeLocationRequest = "location_request"

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