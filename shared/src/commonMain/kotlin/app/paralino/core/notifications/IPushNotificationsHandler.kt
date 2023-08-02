package app.paralino.core.notifications

import kotlinx.serialization.Serializable

interface IPushNotificationsHandler<T: NotificationPayload> {
    fun process(payload: T)
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
    val userId: String
): NotificationPayload