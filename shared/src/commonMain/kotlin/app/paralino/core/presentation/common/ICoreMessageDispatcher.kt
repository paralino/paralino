package app.paralino.core.presentation.common

import app.paralino.core.logg
import app.paralino.core.provider.ICoreMessageDispatcherEvent
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.AppSnackbarResult
import app.paralino.core.ui.components.SnackbarDuration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull

open class CoreMessageDispatcher: ICoreMessageDispatcher {
    override val snackbarChannel: MutableSharedFlow<AppSnackbarData> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val snackbarResultChannel: MutableSharedFlow<AppSnackbarResult> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    /**
     * Returns true if handled
     */
    override fun onMessageEvent(event: ICoreMessageDispatcherEvent): Boolean = false
}

interface ICoreMessageDispatcher {

    val snackbarChannel: MutableSharedFlow<AppSnackbarData>
    val snackbarResultChannel: MutableSharedFlow<AppSnackbarResult>

    fun showSnackbar(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        showSnackbar(AppSnackbarData(text = text, duration = duration))
    }

    suspend fun showSnackbarForResult(data: AppSnackbarData): AppSnackbarResult? {
        showSnackbar(data)
        return snackbarResultChannel.firstOrNull { it.data === data }
    }

    fun showSnackbar(data: AppSnackbarData) {
        val sent = snackbarChannel.tryEmit(data)
        logg.d(
            tag = "snackbar",
            message = "Snackbar sent to ${snackbarChannel.subscriptionCount.value} subscribers: $sent"
        )
    }

    fun onSnackbarResult(result: AppSnackbarResult) {
        logg.d("snackbar", "Got snackbar result: ${result.result}")
        snackbarResultChannel.tryEmit(result)
    }

    fun onMessageEvent(event: ICoreMessageDispatcherEvent): Boolean
}