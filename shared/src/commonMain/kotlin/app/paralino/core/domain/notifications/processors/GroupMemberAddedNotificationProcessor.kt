package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.presentation.settings.generic.NotificationGroupingType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class GroupMemberAddedNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val memberRepo: LocationLocalDataSource,
    private val accessLogSource: AccessLogDataSource,
    private val appPrefs: AppPrefs
): BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<GroupMemberAddedNotificationPayload>()
            ?: return null

        groupRefreshHook?.invoke(
            GroupRefreshHookPayload(
                groupId = payload.groupId,
                cacheTimeDuration = 10.seconds
            )
        )

        val title = try {
            val group: UserGroup? = groupRepo.getGroupOnce(payload.groupId)
            val member = group?.let {
                memberRepo.getMemberOnce(group = it, userId = payload.memberUserId)
            }
//                val groupEmoji = group?.data?.data?.image?.getImageValue(ImageType.emoji)?.let { "$it " } ?: ""
//                val memberEmoji = member?.image?.getImageValue(ImageType.emoji)?.let { "$it " } ?: ""
            val groupName = group?.data?.data?.name //?.let { "$groupEmoji$it" }
            val memberName = member?.name //?.let { "$memberEmoji$it" }

            if (payload.memberUserId == appPrefs.userId) {
                if (groupName.isNullOrBlank()) {
                    MR.strings.group_member_current_add_notification_default.localizedString()
                } else {
                    MR.strings.group_member_current_add_notification.localizedString(groupName)
                }
            } else {
                if (groupName.isNullOrBlank() || memberName.isNullOrBlank()) {
                    MR.strings.group_member_add_notification_default.localizedString()
                } else {
                    MR.strings.group_member_add_notification.localizedString(memberName, groupName)
                }
            }
        } catch (e: Exception) {
            MR.strings.group_member_add_notification_default.localizedString()
        }

        val notificationGroup = when (appPrefs.notificationGrouping) {
            NotificationGroupingType.NONE -> null
            NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
            NotificationGroupingType.TYPE -> "new_member"
            NotificationGroupingType.GROUP -> payload.groupId
            NotificationGroupingType.PERSON -> payload.memberUserId
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = "new_member_${payload}".hashCode(),
            notificationGroupId = notificationGroup,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "group_activity_notification"
        )

        return pushPayload
    }

    @Serializable
    private data class GroupMemberAddedNotificationPayload constructor(
        val groupId: String,
        val memberUserId: String
    )
}