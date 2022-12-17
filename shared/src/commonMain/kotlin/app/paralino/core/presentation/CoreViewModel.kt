package app.paralino.core.presentation

import kotlinx.coroutines.CoroutineScope

expect abstract class CoreViewModel() {
    val coroutineScope: CoroutineScope

    /**
     * Same as Android ViewModel onCleared
     */
    protected open fun onCleared()
    fun cleanup()
}