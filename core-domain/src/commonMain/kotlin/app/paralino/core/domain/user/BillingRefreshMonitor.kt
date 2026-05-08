package app.paralino.core.domain.user

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class BillingRefreshMonitor {
    private val _refreshChannel = Channel<Unit>()
    val refreshEvent = _refreshChannel.receiveAsFlow()

    fun triggerRefresh() {
        _refreshChannel.trySend(Unit)
    }
}
