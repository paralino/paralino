package app.paralino.android.app_update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import app.paralino.android.R
import app.paralino.android.ui.main.MainActivity
import app.paralino.core.MR
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
                title = MR.strings.app_name.getString(context),
                text = MR.strings.app_version_alert_message_updateInstalled.getString(context)
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

        if (!intentTargetsParalinoPackage(intent, context.packageName)) {
            logg.w("app_update", "Ignored install broadcast: unexpected target package")
            updateComponent.appUpdateManager.cancelUpdate()
            return
        }

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                logg.i("app_update", "Request app install - pending user action")
                val confirmationIntent = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_INTENT,
                    Intent::class.java,
                )
                when {
                    confirmationIntent == null -> {
                        updateComponent.appUpdateManager.cancelUpdate()
                    }

                    confirmationIntent.safeToLaunchAsInstallerConfirmation(context) -> {
                        context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }

                    else -> {
                        logg.w(
                            "app_update",
                            "Ignoring EXTRA_INTENT: not resolved to system package installer UI",
                        )
                        updateComponent.appUpdateManager.cancelUpdate()
                    }
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

private fun intentTargetsParalinoPackage(intent: Intent, packageName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val targetPkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) ?: return true
    return targetPkg == packageName
}

private fun Intent.safeToLaunchAsInstallerConfirmation(context: Context): Boolean {
    val resolved = context.resolveInstallationUiActivity(this) ?: return false
    val pkg = resolved.activityInfo.packageName
    val flags = try {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(pkg, 0).flags
    } catch (_: PackageManager.NameNotFoundException) {
        return false
    }
    val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    return isSystem
}

private fun Context.resolveInstallationUiActivity(intent: Intent): ResolveInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
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