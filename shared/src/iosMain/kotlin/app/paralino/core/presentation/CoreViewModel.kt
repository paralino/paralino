package app.paralino.core.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

actual abstract class CoreViewModel actual constructor() {
    actual val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    actual fun cleanup() {
        coroutineScope.cancel()
        onCleared()
    }
    protected actual open fun onCleared() {}
    fun <T> observe(flow: Flow<T>, onChange: (T) -> Unit) {
        flow.onEach { onChange(it) }.launchIn(coroutineScope)
    }
    fun <T> observe(channel: Channel<T>, onChange: (T) -> Unit) {
        observe(channel.receiveAsFlow(), onChange)
    }
}