package app.paralino.android.remote

import io.appwrite.Client

object RemoteClient {
    fun setup(client: Client, debug: Boolean): Client {
        return client.setSelfSigned(false)
    }
}