package app.paralino.core.repo.user

interface IPushProviderManager {
    fun syncPushToken()
    fun syncPushNotifications()
    suspend fun logout()
}