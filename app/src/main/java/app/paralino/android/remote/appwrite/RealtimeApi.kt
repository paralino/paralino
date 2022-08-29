package app.paralino.android.remote.appwrite

import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RealtimeApi constructor(
    private val remoteModule: AppWriteRemoteModule
): IRealtimeApi {


    override fun subscribe(vararg channels: String): Flow<Map<String, Any>> {
        return callbackFlow {

            val subscription = remoteModule.realtime.subscribe(
                channels = channels,
                payloadType = mapOf<String, Any>().javaClass
            ) {
                this.trySend(it.payload)
            }

            awaitClose {
                subscription.close()
            }
        }
    }
}