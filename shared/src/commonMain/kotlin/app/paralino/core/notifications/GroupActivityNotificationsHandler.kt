package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.config
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class GroupActivityNotificationsHandler(
    private val groupRepo: GroupRepository,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val presenter: IGroupActivityNotificationPresenter
) {
    suspend fun processMemberAdded(push: PushNotificationRaw) {

        val payload =
            push.content?.tryParseFromJson<GroupMemberAddedNotificationPayload>() ?: return

        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
        )

        val title = try {
            val group = groupRepo.getGroupWithMembersLocally(payload.groupId).firstOrNull()
            val member = group?.memberByUserId(payload.memberUserId)
//                val groupEmoji = group?.data?.data?.image?.getImageValue(ImageType.emoji)?.let { "$it " } ?: ""
//                val memberEmoji = member?.image?.getImageValue(ImageType.emoji)?.let { "$it " } ?: ""
            val groupName = group?.data?.data?.name //?.let { "$groupEmoji$it" }
            val memberName = member?.name //?.let { "$memberEmoji$it" }

            if (payload.memberUserId == config.userId) {
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

        val notificationId = "new_member_${payload}".hashCode()
        presenter.notify(
            notificationId,
            title,
            message = null,
            push.createdAt.toEpochMilliseconds()
        )
    }

    suspend fun processMemberDeleted(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<GroupMemberDeletedNotificationPayload>() ?: return
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
        )
    }

    suspend fun processLocationSharingStarted(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<GroupMemberDeletedNotificationPayload>() ?: return

        val title = try {
            val group = groupRepo.getGroupWithMembersLocally(payload.groupId).firstOrNull()
            val member = group?.memberByUserId(payload.memberUserId)
            val memberName = member?.name

            if (payload.memberUserId == config.userId) {
                MR.strings.group_member_current_sharing_started_notification.localizedString()
            } else {
                if (memberName.isNullOrBlank()) {
                    MR.strings.group_member_sharing_started_notification_default.localizedString()
                } else {
                    MR.strings.group_member_sharing_started_notification.localizedString(memberName)
                }
            }
        } catch (e: Exception) {
            MR.strings.group_member_sharing_started_notification_default.localizedString()
        }

        val notificationId = "sharing_start_${payload}".hashCode()
        presenter.notify(
            notificationId,
            title,
            message = null,
            push.createdAt.toEpochMilliseconds()
        )
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