package app.paralino.core.presentation

import kotlinx.coroutines.CoroutineScope

abstract class CoreViewModel constructor(
    val messageDispatcher: CoreMessageDispatcher = CoreMessageDispatcher()
) : BaseViewModel(), ICoreMessageDispatcher by messageDispatcher

expect abstract class BaseViewModel constructor() {
    val coroutineScope: CoroutineScope

    /**
     * Same as Android ViewModel onCleared
     */
    protected open fun onCleared()
    fun cleanup()
}
