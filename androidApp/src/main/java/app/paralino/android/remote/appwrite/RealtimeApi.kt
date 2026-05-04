package app.paralino.android.remote.appwrite

import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IRealtimeApi() {
    override fun subscribeToChannels(
        channels: List<String>
    ): Flow<RealtimeResponse<Map<String, Any>>> {
        return callbackFlow {
            val subscription = remoteModule.realtime.subscribe(
                channels = channels.toTypedArray(),
                payloadType = mutableMapOf<String, Any>().javaClass
            ) {
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