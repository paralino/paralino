package app.paralino.core.repo.user

interface IPushNotificationManager {
    fun syncPushToken()
    suspend fun logout()
}