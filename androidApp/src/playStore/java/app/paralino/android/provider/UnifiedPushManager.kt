package app.paralino.android.provider

import app.paralino.android.AppClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UnifiedPushManager(
    private val appClass: AppClass
) : IUnifiedPushManager {

    private val _errorState = MutableStateFlow<UnifiedPushError?>(null)
    override val errorState: StateFlow<UnifiedPushError?> = _errorState

    override fun getDistributors(): List<String> = emptyList()

    override fun registerApp(distributor: String) {}

    override fun unregisterApp() {}

    override fun clearError() {}
}
