package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.unifiedpush.android.connector.UnifiedPush
import kotlin.time.Duration.Companion.days

class UnifiedPushManager(
    private val appClass: AppClass,
    private val appPrefs: AppPrefs
) : IUnifiedPushManager {

    private val _errorState = MutableStateFlow<UnifiedPushError?>(null)
    override val errorState: StateFlow<UnifiedPushError?> = _errorState.asStateFlow()

    override fun getDistributors(): List<String> {
        return UnifiedPush.getDistributors(context = appClass)
    }

    override fun registerApp(distributor: String): Boolean {
        try {
            logg.tag("push_notifications").i("Register new unifiedPush distributor $distributor")
            unregisterApp()
            appPrefs.selectedUnifiedPushDistributor = distributor
            UnifiedPush.saveDistributor(context = appClass, distributor = distributor)
            UnifiedPush.register(context = appClass, messageForDistributor = "Paralino")
            appPrefs.unifiedPushRegistrationTime = nowUTC()
            return true
        } catch (e: Exception) {
            logg.tag("push_notifications").i("Error registering distributor: ${e.message}")
            _errorState.value = UnifiedPushError.Other(e.message ?: "Unknown error")
            return false
        }
    }

    override fun unregisterApp() {
        try {
            logg.tag("push_notifications").i("Deregister unifiedPush distributor ${appPrefs.selectedUnifiedPushDistributor}")
            appPrefs.selectedUnifiedPushDistributor = null
            appPrefs.unifiedPushRegistrationTime = null
            UnifiedPush.unregister(context = appClass)
        } catch (e: Exception) {
            logg.tag("push_notifications").i("Error unregistering distributor: ${e.message}")
            _errorState.value = UnifiedPushError.Other(e.message ?: "Unknown error")
        }
    }

    override fun refresh() {
        try {
            val currentDistributor = UnifiedPush.getAckDistributor(context = appClass)
            val savedDistributor = appPrefs.selectedUnifiedPushDistributor
            logg.tag("push_notifications").i("Refresh unifiedPush distributor, current: ${currentDistributor}, saved: $savedDistributor")
            if (currentDistributor == savedDistributor && !savedDistributor.isNullOrBlank()) {
                if (appPrefs.unifiedPushRegistrationTime?.isOlderThan(2.days) != false) {
                    logg.tag("push_notifications").i("Distributors match, refresh registration")
                    registerApp(distributor = savedDistributor)
                } else {
                    logg.tag("push_notifications").i("Distributors match, no need to refresh")
                }
            } else {
                logg.tag("push_notifications").i("Distributors do not match, unregister")
                unregisterApp()
            }
        } catch (e: Exception) {
            logg.tag("push_notifications").i("Error refreshing distributor: ${e.message}")
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
