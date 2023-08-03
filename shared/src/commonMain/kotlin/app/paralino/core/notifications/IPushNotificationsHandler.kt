package app.paralino.core.notifications

import app.paralino.core.models.raw.PushNotificationRaw
import kotlinx.serialization.Serializable

interface IPushNotificationsHandler<T: NotificationPayload> {
    fun process(push: PushNotificationRaw)
}

interface IPlaceNotificationsHandler: IPushNotificationsHandler<PlaceActivityNotificationPayload>
interface IGroupMemberAddedNotificationsHandler: IPushNotificationsHandler<GroupMemberAddedNotificationPayload>
interface ILocationRequestNotificationsHandler: IPushNotificationsHandler<LocationRequestNotificationPayload>


sealed interface NotificationPayload

@Serializable
data class PlaceActivityNotificationPayload constructor(
    val memberUserId: String,
    val locationId: String,
    val placeId: String,
    val enter: Boolean
): NotificationPayload

@Serializable
data class GroupMemberAddedNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String,
    val ownerUserId: String,
): NotificationPayload

@Serializable
data class LocationRequestNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
): NotificationPayload