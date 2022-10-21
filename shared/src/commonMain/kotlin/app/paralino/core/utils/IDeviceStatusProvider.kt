package app.paralino.core.utils

interface IDeviceStatusProvider {
    val batteryLevel: Int
    suspend fun logout()
}