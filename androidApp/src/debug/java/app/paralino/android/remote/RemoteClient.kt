package app.paralino.android.remote

import io.appwrite.Client
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RemoteClient {
    fun setup(client: Client, debug: Boolean): Client {
        val interceptor = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        client.http = client
            .http
            .newBuilder()
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
        return client
    }
}