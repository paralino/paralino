package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.core.db.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UnifiedPushManager(
    private val appClass: AppClass,
    private val appPrefs: AppPrefs
) : IUnifiedPushManager {

    private val _errorState = MutableStateFlow<UnifiedPushError?>(null)
    override val errorState: StateFlow<UnifiedPushError?> = _errorState

    override fun getDistributors(): List<String> = emptyList()
    override fun registerApp(distributor: String): Boolean = false
    override fun unregisterApp() {}
    override fun refresh() {}
    override fun emitError(error: UnifiedPushError) {}
    override fun clearError() {}
}
