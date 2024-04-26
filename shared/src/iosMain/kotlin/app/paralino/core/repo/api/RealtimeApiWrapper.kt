package app.paralino.core.repo.api

import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.provider.IConnectionStatusProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApiWrapperIOS(
    val api: IRealtimeApiIOS,
    private val connectionStatus: IConnectionStatusProvider
) : IRealtimeApi() {

    override fun subscribeWithFilter(
        channels: List<String>,
        filters: List<String>
    ): Flow<RealtimeResponse<Map<String, Any>>> {
        return callbackFlow {
            val action = if (connectionStatus.initAndGetStatus(forceCheck = true).isOnline) {
                api.subscribeWithFilter(
                    channels = channels,
                    filters = filters,
                    onResult = { trySend(it) }
                )
            } else {
                RealtimeSubscriptionCallbackIOS()
            }

            awaitClose { action.close?.invoke() }
        }
    }

}

interface IRealtimeApiIOS {
    suspend fun subscribeWithFilter(
        channels: List<String>,
        filters: List<String> = listOf(),
        onResult: (RealtimeResponse<Map<String, Any>>) -> Unit
    ): RealtimeSubscriptionCallbackIOS
}

class RealtimeSubscriptionCallbackIOS(var close: (() -> Unit)? = null)