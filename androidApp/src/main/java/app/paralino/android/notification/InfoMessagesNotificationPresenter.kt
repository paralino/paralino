package app.paralino.android.notification

import android.Manifest
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
import app.paralino.android.ui.main.MainActivity
import app.paralino.core.MR
import app.paralino.core.logg
import app.paralino.core.domain.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.provider.InAppLogger
import app.paralino.core.ui.theme.AppColors

class InfoMessagesNotificationPresenter(
    private val context: Context,
    inAppLogger: InAppLogger
) : IInfoMessagesNotificationPresenter(inAppLogger) {

    private val channelId = "system_info_channel"
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    override fun showPushNotification(payload: Payload) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logg.w("push_notifications", "Missing push notification permission!")
            return
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
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setSubText(payload.timeInfo)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSound(null)
            .setGroup(payload.notificationGroupId)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .setColor(notificationColor)
            .setColorized(true)
            .setShowWhen(true)
            .setWhen(payload.timeMillis ?: System.currentTimeMillis())

        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        notificationManager.notify(payload.notificationId, notification)

        if (payload.notificationGroupId != null) {
            val summaryNotification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(payload.title)
                // Content text to support devices running API level < 24 - ignored since we are not running on < 24.
                .setContentText("New notifications")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setStyle(NotificationCompat.InboxStyle().setSummaryText(payload.timeInfo))
                .setGroup(payload.notificationGroupId)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .setGroupSummary(true)
                .build()
            notificationManager.notify(payload.notificationGroupId.hashCode(), summaryNotification)
        }
    }

    private fun buildNotificationIntent(context: Context): Intent {
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
            context.getString(MR.strings.notifications_channel_info_message.resourceId),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.setShowBadge(true)
        channel.setSound(null, null)
        channel.enableVibration(true)
        channel.enableLights(true)
        notificationManager?.createNotificationChannel(channel)
    }

    fun notifyLocationServiceError(
        context: Context,
        backgroundLocationError: Boolean
    ) {
        val message =
            if (backgroundLocationError) {
                MR.strings.error_location_background_permission_missing_notification_message.getString(
                    context
                )
            } else {
                MR.strings.error_location_sharing_start_notification_message.getString(context)
            }
        notify(
            IPushNotificationPresenter.Payload(
                notificationId = 54382,
                notificationGroupId = IPushNotificationPresenter.notificationGroupSystem,
                title = MR.strings.error_something_wrong.getString(context),
                message = message,
                timeMillis = null,
                timeInfo = null
            )
        )
    }

    fun notifyNotificationSocketServiceError(
        context: Context,
        backgroundStartError: Boolean
    ) {
        val message = MR.strings.error_notification_service_error_message.getString(context)
        notify(
            IPushNotificationPresenter.Payload(
                notificationId = 54343,
                notificationGroupId = IPushNotificationPresenter.notificationGroupSystem,
                title = MR.strings.error_something_wrong.getString(context),
                message = message,
                timeMillis = null,
                timeInfo = null
            )
        )
    }
}