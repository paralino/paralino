package app.paralino.android.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import app.paralino.android.R
import app.paralino.core.resources.moko.*
import app.paralino.core.ui.theme.AppColors

sealed class SyncNotificationProvider {

    protected abstract val channelId: String
    protected abstract val channelDescriptionRes: Int
    abstract val notificationId: Int

    protected open val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    fun create(context: Context): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(channelDescriptionRes))
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
            context.getString(channelDescriptionRes),
            NotificationManager.IMPORTANCE_MIN
        )

        channel.setShowBadge(false)
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)
        notificationManager?.createNotificationChannel(channel)
    }

    data object DataSyncNotification : SyncNotificationProvider() {
        override val channelId: String = "data_sync_channel"
        override val channelDescriptionRes: Int =
            MR.strings.notifications_channel_data_sync.resourceId
        override val notificationId: Int = 6724
    }

    data object PushSyncNotification : SyncNotificationProvider() {
        override val channelId: String = "push_sync_channel"
        override val channelDescriptionRes: Int =
            MR.strings.notifications_channel_push_sync.resourceId
        override val notificationId: Int = 6725
    }
}