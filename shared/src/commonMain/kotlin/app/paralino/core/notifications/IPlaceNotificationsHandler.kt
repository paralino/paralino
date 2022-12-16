package app.paralino.core.notifications

import kotlinx.serialization.Serializable

interface IPlaceNotificationsHandler {
    fun process(payload: PlaceActivityNotificationPayload)
}

@Serializable
data class PlaceActivityNotificationPayload constructor(
    val memberUserId: String,
    val memberId: String,
    val locationId: String,
    val placeId: String,
    val enter: Boolean
)