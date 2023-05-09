package app.paralino.android.remote

import io.appwrite.Client
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object RemoteClient {
    fun setup(client: Client, debug: Boolean): Client {
        return client.setSelfSigned(false)
    }

    private fun okHttpClient(debug: Boolean): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}