package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.unifiedpush.android.connector.UnifiedPush

class UnifiedPushManager(
    private val appClass: AppClass,
    private val appPrefs: AppPrefs
) : IUnifiedPushManager {

    private val _errorState = MutableStateFlow<UnifiedPushError?>(null)
    override val errorState: StateFlow<UnifiedPushError?> = _errorState.asStateFlow()

    override fun getDistributors(): List<String> {
        return UnifiedPush.getDistributors(context = appClass)
    }

    override fun registerApp(distributor: String) {
        try {
            logg.tag("push_notifications").i("Register new unifiedPush distributor $distributor")
            unregisterApp()
            appPrefs.selectedUnifiedPushDistributor = distributor
            UnifiedPush.saveDistributor(context = appClass, distributor = distributor)
            UnifiedPush.register(context = appClass, messageForDistributor = "Paralino")
        } catch (e: Exception) {
            _errorState.value = UnifiedPushError.Other(e.message ?: "Unknown error")
        }
    }

    override fun unregisterApp() {
        try {
            logg.tag("push_notifications").i("Deregister unifiedPush distributor ${appPrefs.selectedUnifiedPushDistributor}")
            appPrefs.selectedUnifiedPushDistributor = null
            UnifiedPush.unregister(context = appClass)
        } catch (e: Exception) {
            _errorState.value = UnifiedPushError.Other(e.message ?: "Unknown error")
        }
    }

    override fun clearError() {
        _errorState.value = null
    }

    override fun emitError(error: UnifiedPushError) {
        _errorState.value = error
    }
}
