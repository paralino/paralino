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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class GroupActivityNotificationsHandler(
    private val groupRepo: GroupRepository,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val scopeProvider: ICoroutineScopeProvider,
    private val presenter: IGroupActivityNotificationPresenter
) {
    suspend fun processMemberAdded(push: PushNotificationRaw) {

        val payload =
            push.content?.tryParseFromJson<GroupMemberAddedNotificationPayload>() ?: return

        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
        )

        scopeProvider.userSessionIOScope.launch {
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
                        MR.strings.group_member_add_notification.localizedString(
                            memberName,
                            groupName
                        )
                    }
                }
            } catch (e: Exception) {
                MR.strings.group_member_add_notification_default.localizedString()
            }

            val notificationId = payload.hashCode()
            presenter.notify(
                notificationId,
                title,
                message = null,
                push.createdAt.toEpochMilliseconds()
            )
        }
    }

    suspend fun processMemberDeleted(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<GroupMemberDeletedNotificationPayload>() ?: return
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 10.seconds.inWholeMilliseconds
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