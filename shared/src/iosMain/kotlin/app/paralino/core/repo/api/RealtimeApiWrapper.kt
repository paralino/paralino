package app.paralino.core.repo.api

import app.paralino.core.models.base.RealtimeResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApiWrapperIOS(
    val api: IRealtimeApiIOS
) : IRealtimeApi() {

    override fun subscribeWithFilter(
        channels: List<String>,
        filters: List<String>
    ): Flow<RealtimeResponse<Map<String, Any>>> {
        return callbackFlow {
            val action = api.subscribeWithFilter(
                channels = channels,
                filters = filters,
                onResult = { trySend(it) }
            )
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