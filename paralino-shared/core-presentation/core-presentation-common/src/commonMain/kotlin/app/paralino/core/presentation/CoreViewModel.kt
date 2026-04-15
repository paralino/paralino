package app.paralino.core.presentation

import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.CoreMessageDispatcher
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class CoreViewModel constructor(
    val messageDispatcher: CoreMessageDispatcher = CoreMessageDispatcher()
) : BaseViewModel(), ICoreMessageDispatcher by messageDispatcher {
    override fun showSnackbar(text: String, duration: SnackbarDuration) {
        coroutineScope.launch(Dispatchers.Main) {
            super.showSnackbar(text, duration)
        }
    }

    override fun showSnackbar(data: AppSnackbarData) {
        coroutineScope.launch(Dispatchers.Main) {
            super.showSnackbar(data)
        }
    }
}

expect abstract class BaseViewModel constructor() {
    val coroutineScope: CoroutineScope

    /**
     * Same as Android ViewModel onCleared
     */
    protected open fun onCleared()
    fun cleanup()
}
