package app.paralino.android.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.paralino.android.notification.NotificationSocketServiceLauncher
import app.paralino.core.logg
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocationStartBroadcastReceiver: BroadcastReceiver() {

    private val receiverComponent: BootReceiverComponent by lazy { BootReceiverComponent() }

    override fun onReceive(context: Context?, intent: Intent?) {

        context ?: return
        intent ?: return

        logg.v("location_service", "Waking the app from action: ${intent.action}")

        val appUpdated = intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        val phoneBooted = intent.action == Intent.ACTION_BOOT_COMPLETED

        if (appUpdated || phoneBooted) {
            receiverComponent.launchService(action = intent.action)
        }
    }
}

class BootReceiverComponent() : KoinComponent {
    private val locationServiceLauncher: LocationServiceLauncher by inject()
    private val notificationServiceLauncher: NotificationSocketServiceLauncher by inject()

    fun launchService(action: String?) {
        locationServiceLauncher.startLocationService(action = action)
        notificationServiceLauncher.startNotificationService(action = action)
    }
}