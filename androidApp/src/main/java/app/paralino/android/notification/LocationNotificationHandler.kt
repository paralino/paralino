package app.paralino.android.notification

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import app.paralino.core.utils.ConditionErrorType
import app.paralino.android.R
import app.paralino.android.ui.MainActivity
import app.paralino.android.ui.components.nameRes
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.utils.extensions.attributedBold

class LocationNotificationHandler(
    private val context: Context
) {

    private val channelId = "location_active_channel"
    val notificationId = 9537
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    fun notify(context: Context, groupCount: Int, errors: List<ConditionErrorType>): Notification {

        val notificationManager = NotificationManagerCompat.from(context)

        val intent = buildNotificationIntent(context)
        intent.apply { flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED }
        val pendingIntent = TaskStackBuilder.create(context)
            .apply { addNextIntentWithParentStack(intent) }
            .getPendingIntent(
                100,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val summary = if (errors.isEmpty()) {
            context.getString(R.string.notifications_sharing_active_title)
        } else {
            context.getString(R.string.notifications_sharing_error_title)
        }

        val content = if (errors.isEmpty()) {
            context.resources.getQuantityString(
                R.plurals.notifications_sharing_active_message, groupCount, groupCount
            )
        } else {
            val errorContent = errors.map {
                context.getString(it.nameRes)
            }.joinToString(separator = "**\n- **", prefix = "- **", postfix = "**")
                .attributedBold(context)

            context.getString(R.string.notifications_sharing_error_message, errorContent)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setStyle(NotificationCompat.BigTextStyle())
            .setSubText(summary)
            .setContentText(content)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setSound(null)
            .setColor(notificationColor)
            .setColorized(true)


        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    fun removeNotification() {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    private fun buildNotificationIntent(context: Context): Intent {
        // TODO deeplink to map
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
            context.getString(R.string.notifications_channel_active_location),
            NotificationManager.IMPORTANCE_MIN
        )

        channel.setShowBadge(false)
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)
        notificationManager?.createNotificationChannel(channel)
    }

}