package app.paralino.core.repo.user

interface IPushProviderManager {
    fun syncPushToken()
    suspend fun logout()
}