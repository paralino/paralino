package app.paralino.core.domain.notifications

import app.paralino.core.MR
import app.paralino.core.appPrefs
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GroupActivityNotificationsHandler(
    private val groupRepo: UserGroupLocalDataSource,
    private val memberRepo: LocationLocalDataSource,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val valueFormatter: IValueFormatter
) {
    suspend fun processMemberAdded(push: PushNotificationRaw): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<GroupMemberAddedNotificationPayload>()
            ?: return null

        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
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

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = "new_member_${payload}".hashCode(),
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "group_activity_notification"
        )

        return pushPayload
    }

    suspend fun processMemberDeleted(push: PushNotificationRaw): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<GroupMemberDeletedNotificationPayload>()
            ?: return null
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
        )
        return null
    }

    suspend fun processLocationSharingStarted(push: PushNotificationRaw): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<LocationSharingStartedNotificationPayload>()
            ?: return null

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
}

@Serializable
data class GroupMemberAddedNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
)

@Serializable
data class GroupMemberDeletedNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
)

@Serializable
data class LocationSharingStartedNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
)