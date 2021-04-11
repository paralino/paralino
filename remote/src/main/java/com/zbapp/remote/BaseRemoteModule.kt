package com.zbapp.remote

import com.etebase.client.Client
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object BaseRemoteModule {

    fun getEtebaseClient(serverUrl: String): Client {
        return Client.create(createHttpClient(), serverUrl)
    }

    private fun createHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE

        val builder = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        return builder.build()
    }

    val formatter = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}