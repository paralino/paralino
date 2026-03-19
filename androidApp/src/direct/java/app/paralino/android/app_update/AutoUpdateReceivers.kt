package app.paralino.android.app_update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import app.paralino.android.R
import app.paralino.android.ui.main.MainActivity
import app.paralino.core.logg
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class InstalledReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        logg.i("app_update", "App update installed")
        context ?: return
        if (intent?.action?.equals(Intent.ACTION_MY_PACKAGE_REPLACED) == true) {
            try {
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
            }
            AppUpdateComponent.sendNotification(
                context = context,
                title = "Paralino",
                text = "New update installed!"
            )
        }
    }
}

class InstallRequestReceiver : BroadcastReceiver() {

    private val updateComponent by lazy { AppUpdateComponent() }

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null) {
            return
        }

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                logg.i("app_update", "Request app install - pending user action")
                intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } ?: let {
                    updateComponent.appUpdateManager.cancelUpdate()
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                logg.i("app_update", "New package installed")
            }

            else -> {
                logg.w("app_update", "Package update failed, status: $status")
                AppUpdateComponent.sendNotification(
                    context = context,
                    title = "Update failed",
                    text = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        ?: "System failed to update Paralino app."
                )
                updateComponent.appUpdateManager.cancelUpdate()
            }
        }
    }
}

class AppUpdateComponent() : KoinComponent {
    val appUpdateManager: AppUpdateManager by inject()

    companion object {
        fun sendNotification(
            context: Context,
            title: String,
            text: String
        ) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val channelId = "auto_update_channel"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setStyle(NotificationCompat.BigTextStyle())
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelId,
                "Auto Update",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            notificationManager.notify(title.hashCode(), notificationBuilder.build())
        }
    }
}