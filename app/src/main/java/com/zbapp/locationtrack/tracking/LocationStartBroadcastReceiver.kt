package com.zbapp.locationtrack.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import logv

class LocationStartBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val appUpdated = intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        val phoneBooted = intent?.action == Intent.ACTION_BOOT_COMPLETED

        logv("location_service", "got broadcast: ${intent?.action}")
        if(appUpdated || phoneBooted) {
            try {
                logv("location_service", "starting location service from action: ${intent?.action}")
                // TODO review and test if starting service from contract subscription is enough
//                context?.let { LocationService.start(context) }
            } catch (e: IllegalStateException) {
                Logger.tag("location_service").e("Not allowed to start location service from action: ${intent?.action}", e)
            }
        }
    }

}