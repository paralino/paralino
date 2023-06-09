package app.paralino.android.notification

import android.Manifest
import android.app.*
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
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.models.data
import app.paralino.core.notifications.IPlaceNotificationsHandler
import app.paralino.core.notifications.PlaceActivityNotificationPayload
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PlaceNotificationHandler(
    private val context: Context,
    private val locationRepo: LocationRepository,
    private val placeRepo: PlacesRepository,
    private val scopeProvider: ICoroutineScopeProvider,
): IPlaceNotificationsHandler {

    private val channelId = "place_activity_channel"
    private val notificationColor: Int by lazy {
        AppColors.lightThemePrimary.toArgb()
    }

    override fun process(payload: PlaceActivityNotificationPayload) {
        scopeProvider.userSessionIOScope.launch {

            var placeName: String? = null
            var userName: String? = null

            try {
                val place = placeRepo.getPlaceLocally(payload.placeId).firstOrNull()
                placeName = place?.data?.data?.name
                userName = locationRepo.getMember(place!!.groupId, payload.memberUserId)?.name
            } catch (e: Exception) { }

            val title = if (placeName.isNullOrBlank() || userName.isNullOrBlank()) {
                context.getString(R.string.place_notification_default)
            } else {
                if (payload.enter)
                    context.getString(R.string.place_notification_enter, userName, placeName)
                else
                    context.getString(R.string.place_notification_exit, userName, placeName)
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
            .setGroup("place_notify")
            .setColor(notificationColor)
            .setColorized(true)


        registerNotificationChannel(context)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        return notification
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
            context.getString(R.string.notifications_channel_place_alert),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.setShowBadge(true)
        channel.setSound(null, null)
        channel.enableVibration(true)
        channel.enableLights(true)
        notificationManager?.createNotificationChannel(channel)
    }

}

