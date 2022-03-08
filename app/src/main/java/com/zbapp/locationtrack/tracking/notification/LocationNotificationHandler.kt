package com.zbapp.locationtrack.tracking.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainActivity

class LocationNotificationHandler (
    private val context: Context
) {

    private val channelId = "location_active_channel"
    val notificationId = 9537
    private val notificationColor: Int by lazy {
        ContextCompat.getColor(context, R.color.theme_light)
    }

    fun notify(context: Context, extra: String = ""): Notification {

        val notificationManager = NotificationManagerCompat.from(context)

        val intent = buildNotificationIntent(context)
        intent.apply { flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED }
        val pendingIntent = TaskStackBuilder.create(context)
            .apply { addNextIntentWithParentStack(intent) }
            .getPendingIntent(100, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.notifications_active_location_title))
            .setContentText(extra)
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSound(null)
            .setColor(notificationColor)
            .setColorized(true)


        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    fun removeNotification() {
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    private fun buildNotificationIntent(context: Context): Intent {
        // TODO deeplink to map
        return Intent(context, MainActivity::class.java)
    }

    private fun registerNotificationChannel(context : Context) {
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager?.getNotificationChannel(channelId) != null) {
            return
        }

        val channel = NotificationChannel(channelId,
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