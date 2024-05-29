package app.paralino.android.remote

import io.appwrite.Client
import okhttp3.logging.HttpLoggingInterceptor

object RemoteClient {
    fun setup(client: Client, debug: Boolean): Client {
        val interceptor = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        client.http = client
            .http
            .newBuilder()
            .addInterceptor(interceptor)
            .build()
        return client
    }
}