package app.paralino.android.remote

import io.appwrite.Client
//import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RemoteClient {
    fun setup(client: Client, logRequests: Boolean): Client {
//        val interceptor = HttpLoggingInterceptor().apply {
//            level = if (logRequests) HttpLoggingInterceptor.Level.BASIC
//            else HttpLoggingInterceptor.Level.NONE
//        }
        client.http = client
            .http
            .newBuilder()
//            .addInterceptor(interceptor)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return client
    }
}