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
import androidx.core.net.toUri
import app.paralino.android.R
import app.paralino.android.ui.main.MainActivity
import app.paralino.android.utils.extensions.attributedBold
import app.paralino.core.MR
import app.paralino.core.ui.components.nameRes
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType

class PushServiceNotificationHandler(
    private val context: Context
) {

    val notificationId = 9290
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    fun notify(
        context: Context,
        errors: List<ConditionErrorType>,
        connectionState: PushServiceConnectionState?,
        lastSyncTime: String?
    ): Notification {

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
            when (connectionState) {
                PushServiceConnectionState.Scheduled,
                PushServiceConnectionState.Connecting -> {
                    context.getString(MR.strings.notifications_socketService_status_warning.resourceId)
                }

                PushServiceConnectionState.Disconnected -> {
                    context.getString(MR.strings.notifications_socketService_status_error.resourceId)
                }

                null,
                PushServiceConnectionState.Connected -> {
                    context.getString(MR.strings.notifications_socketService_status_active.resourceId)
                }
            }
        } else {
            context.getString(MR.strings.notifications_socketService_status_error.resourceId)
        }

        val content = if (errors.isEmpty()) {
            when (connectionState) {
                PushServiceConnectionState.Scheduled,
                PushServiceConnectionState.Connecting -> {
                    context.getString(MR.strings.notifications_socketService_message_connecting.resourceId)
                }

                PushServiceConnectionState.Disconnected -> {
                    context.getString(
                        MR.strings.notifications_socketService_message_errors.resourceId,
                        context.getString(MR.strings.notifications_socketService_message_disconnected.resourceId)
                    )
                }

                null,
                PushServiceConnectionState.Connected -> {
                    if (lastSyncTime != null) {
                        context.getString(MR.strings.notifications_socketService_message_lastSync.resourceId, lastSyncTime)
                    } else {
                        ""
                    }
                }
            }
        } else {
            val errorContent =
                errors.joinToString(separator = "**\n- **", prefix = "- **", postfix = "**") {
                    context.getString(it.nameRes.resourceId)
                }
                    .attributedBold(context)

            context.getString(MR.strings.notifications_socketService_message_errors.resourceId, errorContent)
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
            .setPriority(
                if (errors.isEmpty())
                    NotificationCompat.PRIORITY_MIN
                else
                    NotificationCompat.PRIORITY_HIGH
            )
            .setColorized(true)


        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, notification)
        }
        return notification
    }

    fun removeNotification() {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    private fun buildNotificationIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            this.data = DeeplinkType.PushNotificationServiceScreen.buildUrl().toUri()
        }
    }

    private fun registerNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager?.getNotificationChannel(channelId) != null) {
            return
        }

        val channel = NotificationChannel(
            channelId,
            context.getString(MR.strings.notifications_channel_socket_service.resourceId),
            NotificationManager.IMPORTANCE_MIN
        )

        channel.setShowBadge(false)
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.enableLights(false)
        notificationManager?.createNotificationChannel(channel)
    }

    companion object Companion {
        const val channelId = "notification_socket_active_channel"
    }
}