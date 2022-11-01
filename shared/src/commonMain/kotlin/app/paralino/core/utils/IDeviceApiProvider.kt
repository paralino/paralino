package app.paralino.core.utils

interface IDeviceApiProvider {
    val batteryLevel: Int
    val randomUUID: String
    suspend fun logout()
}