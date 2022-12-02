package app.paralino.core.utils

import app.paralino.core.models.base.DeviceType

interface IDeviceApiProvider {
    val batteryLevel: Int
    val randomUUID: String
    val deviceType: DeviceType
    suspend fun logout()
    fun initialize()
}