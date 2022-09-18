package app.paralino.android.utils

import app.paralino.android.AppClass
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.core.utils.IDeviceStatusProvider

class DeviceStatusProvider(
    private val appClass: AppClass
): IDeviceStatusProvider {
    override val batteryLevel: Int
        get() = appClass.batterLevel
}