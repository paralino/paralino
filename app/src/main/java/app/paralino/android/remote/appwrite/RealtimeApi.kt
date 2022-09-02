package app.paralino.android.remote.appwrite

import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApi constructor(
    private val remoteModule: AppWriteRemoteModule
): IRealtimeApi {

    override fun subscribe(vararg channels: String): Flow<Map<String, Any>> {
        return subscribeWithFilter(channels.toList(), listOf())
    }

    override fun subscribeWithFilter(channel: String, vararg filter: String): Flow<Map<String, Any>> {
        return subscribeWithFilter(listOf(channel), filter.toList())
    }

    override fun subscribeWithFilter(
        channel: String,
        filters: List<String>
    ): Flow<Map<String, Any>> {
        return subscribeWithFilter(listOf(channel), filters)
    }

    override fun subscribeWithFilter(channels: List<String>, filters: List<String>): Flow<Map<String, Any>> {
        return callbackFlow {

            val subscription = remoteModule.realtime.subscribe(
                channels = channels.toTypedArray(),
                payloadType = mutableMapOf<String, Any>().javaClass
            ) {
                if(filters.isEmpty() || it.events.any { event -> filters.any { it == event } })
                    this.trySend(it.payload)
            }

            awaitClose {
                subscription.close()
            }
        }
    }
}