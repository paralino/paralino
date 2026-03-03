package app.paralino.android.provider

import kotlinx.coroutines.flow.StateFlow

interface IUnifiedPushManager {
    val errorState: StateFlow<UnifiedPushError?>
    
    fun getDistributors(): List<String>
    fun registerApp(distributor: String): Boolean
    fun unregisterApp()
    fun refresh()
    fun emitError(error: UnifiedPushError)
    fun clearError()
}

sealed class UnifiedPushError {
    data class RegistrationFailed(val reason: String) : UnifiedPushError()
    data class Other(val message: String) : UnifiedPushError()
}
