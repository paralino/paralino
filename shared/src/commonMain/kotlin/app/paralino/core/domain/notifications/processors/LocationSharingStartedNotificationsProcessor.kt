package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

class LocationSharingStartedNotificationsProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val memberRepo: LocationLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val accessLogSource: AccessLogDataSource,
    private val appPrefs: AppPrefs
): BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<LocationSharingStartedNotificationPayload>()
            ?: return null

        // INFO: no refresh for this notification

        val title = try {
            if (payload.memberUserId == appPrefs.userId) {
                MR.strings.group_member_current_sharing_started_notification.localizedString()
            } else {
                val group = groupRepo.getGroupOnce(payload.groupId)
                val member = group?.let {
                    memberRepo.getMemberOnce(group = it, userId = payload.memberUserId)
                }
                val memberName = member?.name

                if (memberName.isNullOrBlank()) {
                    MR.strings.group_member_sharing_started_notification_default.localizedString()
                } else {
                    MR.strings.group_member_sharing_started_notification.localizedString(memberName)
                }
            }
        } catch (e: Exception) {
            MR.strings.group_member_sharing_started_notification_default.localizedString()
        }

        val timeInfo = if (push.createdAt.isOlderThan(10.minutes)) {
            valueFormatter.formatSimpleDateTime(
                timeUTC = push.createdAt.toLocalDateTime(TimeZone.UTC),
                optionalYear = true,
                optionalMonth = false,
                showTodayInfo = false
            )
        } else {
            null
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = "sharing_start_${payload}".hashCode(),
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = timeInfo,
            loggerTag = "group_activity_notification"
        )

        return pushPayload
    }

    @Serializable
    private data class LocationSharingStartedNotificationPayload constructor(
        val groupId: String,
        val memberUserId: String
    )
}