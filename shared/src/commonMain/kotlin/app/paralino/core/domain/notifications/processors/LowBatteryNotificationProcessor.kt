package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable

class LowBatteryNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val accessLogSource: AccessLogDataSource,
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<LowBatteryNotificationPayload>()
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

        val notificationId = "low_battery_${payload}".hashCode()

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = notificationId,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "low_battery_notification"
        )

        return pushPayload
    }

    @Serializable
    private data class LowBatteryNotificationPayload constructor(
        val memberUserId: String,
        val locationId: String,
        val groupId: String
    )
}