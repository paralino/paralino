package app.paralino.android.remote.appwrite

import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApi constructor(
    private val remoteModule: AppWriteRemoteModule
): IRealtimeApi() {

    override fun subscribeWithFilter(channels: List<String>, filters: List<String>): Flow<RealtimeResponse<Map<String, Any>>> {
        return callbackFlow {

            val subscription = remoteModule.realtime.subscribe(
                channels = channels.toTypedArray(),
                payloadType = mutableMapOf<String, Any>().javaClass
            ) {
                if(filters.isEmpty() || it.events.any { event -> filters.any { it == event } })
                    this.trySend(
                        RealtimeResponse(
                            events = it.events,
                            channels = it.channels,
                            timestamp = it.timestamp,
                            payload = it.payload
                        )
                    )
            }

            awaitClose {
                subscription.close()
            }
        }
    }
}