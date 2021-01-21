package com.zbapp.locationtrack.utils.extensions

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager


val Context.batterLevel: Int get() {
    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}