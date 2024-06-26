package app.paralino.android.remote

import io.appwrite.Client
import java.util.concurrent.TimeUnit

object RemoteClient {
    fun setup(client: Client, debug: Boolean): Client {
        client.http = client
            .http
            .newBuilder()
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return client
    }
}