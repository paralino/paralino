package app.paralino.android.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import app.paralino.android.R
import app.paralino.android.ui.MainActivity
import app.paralino.core.MR
import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.base.ImageType
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.notifications.GroupMemberAddedNotificationPayload
import app.paralino.core.notifications.IGroupMemberAddedNotificationsHandler
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class GroupActivityNotificationHandler(
    private val context: Context,
    private val groupRepo: GroupRepository,
    private val scopeProvider: ICoroutineScopeProvider,
): IGroupMemberAddedNotificationsHandler {

    private val channelId = "group_activity_channel"
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

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

                if(payload.memberUserId == config.userId) {
                    if (groupName.isNullOrBlank()) {
                        context.getString(MR.strings.group_member_current_add_notification_default.resourceId)
                    } else {
                        context.getString(MR.strings.group_member_current_add_notification.resourceId, groupName)
                    }
                } else {
                    if (groupName.isNullOrBlank() || memberName.isNullOrBlank()) {
                        context.getString(MR.strings.group_member_add_notification_default.resourceId)
                    } else {
                        context.getString(MR.strings.group_member_add_notification.resourceId, memberName, groupName)
                    }
                }
            } catch (e: Exception) {
                context.getString(MR.strings.group_member_add_notification_default.resourceId)
            }

            val notificationId = payload.hashCode()
            notify(notificationId, title)
        }
    }

    private fun notify(notificationId: Int, title: String): Notification? {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logg.w("push_notifications", "Missing push notification permission!")
            return null
        }

        val notificationManager = NotificationManagerCompat.from(context)

        val intent = buildNotificationIntent(context)
        intent.apply { flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED }
        val pendingIntent = TaskStackBuilder.create(context)
            .apply { addNextIntentWithParentStack(intent) }
            .getPendingIntent(
                100,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSound(null)
            .setGroup("group_notify")
            .setColor(notificationColor)
            .setColorized(true)


        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    private fun buildNotificationIntent(context: Context): Intent {
        // TODO deeplink to a group
        return Intent(context, MainActivity::class.java)
    }

    private fun registerNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager?.getNotificationChannel(channelId) != null) {
            return
        }

        val channel = NotificationChannel(
            channelId,
            context.getString(MR.strings.notifications_channel_group_activity.resourceId),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.setShowBadge(true)
        channel.setSound(null, null)
        channel.enableVibration(true)
        channel.enableLights(true)
        notificationManager?.createNotificationChannel(channel)
    }

}
