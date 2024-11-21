package app.paralino.android.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.paralino.core.provider.INotificationStatusProvider
import app.paralino.core.provider.NotificationConditionStatus

class NotificationStatusProvider(
    private val context: Context
) : INotificationStatusProvider() {

    init {
        refreshNotificationsStatus()
    }

    override fun createCurrentStatus(): NotificationConditionStatus {
        val isNotificationPermissionAllowed =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }

        return NotificationConditionStatus(
            isNotificationPermissionAllowed = isNotificationPermissionAllowed
        )
    }
}