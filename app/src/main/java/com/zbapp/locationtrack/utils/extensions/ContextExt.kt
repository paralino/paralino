package com.zbapp.locationtrack.utils.extensions

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.BatteryManager


val Context.batterLevel: Int get() {
    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}