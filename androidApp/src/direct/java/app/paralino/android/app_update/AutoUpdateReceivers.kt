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
        logg.v("app_update", "App update installed")
        context ?: return
        if (intent?.action?.equals(Intent.ACTION_MY_PACKAGE_REPLACED) == true) {
            try {
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
            }
            sendNotification(context)
        }
    }

    private fun sendNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = "auto_update_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("New update installed")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(14455, notificationBuilder.build())
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
                logg.v("app_update", "Request app install")
                intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } ?: let {
                    updateComponent.appUpdateManager.cancelUpdate()
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {}
            else -> {
                updateComponent.appUpdateManager.cancelUpdate()
            }
        }
    }
}

class AppUpdateComponent() : KoinComponent {
    val appUpdateManager: AppUpdateManager by inject()
}