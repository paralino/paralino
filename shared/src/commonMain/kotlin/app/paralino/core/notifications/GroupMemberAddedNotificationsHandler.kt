package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.config
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

class GroupMemberAddedNotificationsHandler(
    private val groupRepo: GroupRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val presenter: IGroupMemberAddedNotificationPresenter
) : IPushNotificationsHandler {

    override fun process(push: PushNotificationRaw) {

        val payload = push.content?.let {
            BaseRemoteModule.formatter.decodeFromString<GroupMemberAddedNotificationPayload>(it)
        } ?: return

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
            presenter.notify(notificationId, title, message = null, push.createdAt.toEpochMilliseconds())
        }
    }
}

@Serializable
data class GroupMemberAddedNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
)
