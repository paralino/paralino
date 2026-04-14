package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.models.settings.NotificationGroupingType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable

class LowBatteryNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val personalDeviceLocal: PersonalDevicesLocalDataSource,
    private val accessLogSource: AccessLogDataSource,
    private val appPrefs: AppPrefs
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val result = parseNotificationContent(push = push)
            ?: return null

        val notificationId = "low_battery_${result.groupId.id}_${result.memberUserId}".hashCode()

        val notificationGroup = when (appPrefs.notificationGrouping) {
            NotificationGroupingType.NONE -> null
            NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
            NotificationGroupingType.TYPE -> "low_battery"
            NotificationGroupingType.GROUP -> result.groupId.id
            NotificationGroupingType.PERSON -> result.memberUserId
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = notificationId,
            notificationGroupId = notificationGroup,
            title = result.title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "low_battery_notification"
        )

        return pushPayload
    }

    private suspend fun parseNotificationContent(push: PushNotificationRaw): LowBatteryNotificationResult? {
        val content = if (push.pushType == PushNotificationType.PERSONAL_LOW_BATTERY_ALERT) {
            val payload = push.content?.tryParseFromJson<PersonalLowBatteryNotificationPayload>()
                ?: return null

            val device = personalDeviceLocal.getUserDeviceByDeviceIdOnce(deviceId = payload.deviceId)
            val deviceName = device?.deviceNameOrNull
            val title = if (deviceName.isNullOrBlank()) {
                MR.strings.personalSpace_low_battery_notification_default.localizedString()
            } else {
                MR.strings.personalSpace_low_battery_notification.localizedString(deviceName)
            }

            LowBatteryNotificationResult(
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                memberUserId = appPrefs.userId,
                title = title
            )
        } else {
            val payload = push.content?.tryParseFromJson<GroupLowBatteryNotificationPayload>()
                ?: return null

            var groupName: String? = null
            var userName: String? = null

            try {
                val group: UserGroup? = groupRepo.getGroupOnce(payload.groupId)
                groupName = group?.nameOrNull
                userName = groupRepo.getMemberNameOnce(
                    groupId = payload.groupId,
                    userId = payload.memberUserId
                )
            } catch (_: Exception) {
            }

            val title = if (groupName.isNullOrBlank() || userName.isNullOrBlank()) {
                MR.strings.group_low_battery_notification_default.localizedString()
            } else {
                MR.strings.group_low_battery_notification.localizedString(userName)
            }

            LowBatteryNotificationResult(
                groupId = payload.groupId.asGroupOrSpaceId(),
                memberUserId = payload.memberUserId,
                title = title
            )
        }

        return content
    }

    private data class LowBatteryNotificationResult constructor(
        val groupId: GroupOrSpaceId,
        val memberUserId: String,
        val title: String
    )

    @Serializable
    private data class GroupLowBatteryNotificationPayload constructor(
        val memberUserId: String,
        val locationId: String,
        val groupId: String
    )

    @Serializable
    private data class PersonalLowBatteryNotificationPayload constructor(
        val deviceId: String
    )
}