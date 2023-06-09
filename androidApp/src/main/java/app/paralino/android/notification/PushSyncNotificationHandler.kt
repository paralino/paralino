package app.paralino.android.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import app.paralino.android.R
import app.paralino.core.ui.theme.AppColors

object PushSyncNotificationHandler {

    private const val channelId = "push_sync_channel"
    const val notificationId = 6725
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    fun create(context: Context): Notification {

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.notifications_channel_push_sync))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSound(null)
            .setColor(notificationColor)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setColorized(true)

        registerNotificationChannel(context)

        return notificationBuilder.build()
    }

    private fun registerNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager?.getNotificationChannel(channelId) != null) {
            return
        }

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notifications_channel_push_sync),
            NotificationManager.IMPORTANCE_MIN
        )

        channel.setShowBadge(false)
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)
        notificationManager?.createNotificationChannel(channel)
    }

}