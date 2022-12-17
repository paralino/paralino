package app.paralino.core.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual abstract class CoreViewModel actual constructor() {
    actual val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    actual fun cleanup() {
        coroutineScope.cancel()
        onCleared()
    }
    protected actual open fun onCleared() {}
}