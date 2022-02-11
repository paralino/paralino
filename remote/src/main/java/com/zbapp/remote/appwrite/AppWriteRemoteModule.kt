package com.zbapp.remote.appwrite

import android.app.Application
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.utils.BaseRemoteException
import com.zbapp.core.utils.ConnectionException
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Database
import io.appwrite.services.Functions
import io.appwrite.services.Teams
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.UnknownHostException

class AppWriteRemoteModule(
    private val app: Application,
    val config: BackendConfig
) {

    internal val account: Account by lazy { Account(client) }
    internal val teams: Teams by lazy { Teams(client) }
    internal val database: Database by lazy { Database(client) }
    internal val functions: Functions by lazy { Functions(client) }
    val uniqueIdPlaceholder = "unique()"

    private val client: Client by lazy {
        Client(app, selfSigned = false)
            .setEndpoint(config.serverUrl)
            .setProject(config.projectId)
            .setSelfSigned(status = false, okHttpClient(config.debug))
    }

    private fun okHttpClient(debug: Boolean): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    inline fun <T> execute(action: AppWriteRemoteModule.() -> T): T {
        try {
            return action(this)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    companion object {
        fun mapException(exception: Exception): Exception {
            return (exception as? AppwriteException)?.let {
                BaseRemoteException.fromCode(
                    it.code,
                    it.message,
                    it.cause ?: Throwable(it.response)
                )
            } ?: let {
                if (exception is UnknownHostException) {
                    ConnectionException(exception.message, code = null, exception)
                } else exception
            }
        }
    }

}