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
import app.paralino.core.resources.moko.*
import app.paralino.core.logg
import app.paralino.core.domain.notifications.IPlaceActivityNotificationPresenter
import app.paralino.core.provider.InAppLogger
import app.paralino.core.ui.theme.AppColors

class PlaceNotificationPresenter(
    private val context: Context,
    inAppLogger: InAppLogger
) : IPlaceActivityNotificationPresenter(inAppLogger) {

    private val channelId = "place_activity_channel"
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
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setSubText(payload.timeInfo)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
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
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .build()
            notificationManager.notify(payload.notificationGroupId.hashCode(), summaryNotification)
        }
    }

    private fun buildNotificationIntent(context: Context): Intent {
        // TODO deeplink to a place
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
            context.getString(MR.strings.notifications_channel_place_alert.resourceId),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.setShowBadge(true)
        channel.setSound(null, null)
        channel.enableVibration(true)
        channel.enableLights(true)
        notificationManager?.createNotificationChannel(channel)
    }
}
