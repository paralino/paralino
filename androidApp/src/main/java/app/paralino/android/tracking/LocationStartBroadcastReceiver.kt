package app.paralino.android.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.paralino.core.logg

class LocationStartBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appUpdated = intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        val phoneBooted = intent?.action == Intent.ACTION_BOOT_COMPLETED

        logg.v("location_service", "Waking the app from action: ${intent?.action}")
    }
}