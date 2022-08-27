package app.paralino.android.remote.appwrite

import android.app.Application
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.RemoteConnectionException
import app.paralino.core.utils.exceptions.UnauthorizedException
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.UnknownHostException

class AppWriteRemoteModule(
    private val app: Application,
    val config: BackendConfig,
    val sessionMonitor: UserSessionMonitor,
) {

    internal val account: Account by lazy { Account(client) }
    internal val databaseData: Databases by lazy { Databases(client, BackendDatabaseType.DATA.id) }
    internal val databaseKeys: Databases by lazy { Databases(client, BackendDatabaseType.KEYS.id) }
    internal val functions: Functions by lazy { Functions(client) }
    val uniqueIdPlaceholder = "unique()"

    private val client: Client by lazy {
        Client(app, selfSigned = false)
            .setEndpoint(config.endpoint)
            .setProject(config.projectId)
            .setSelfSigned(status = false, okHttpClient(config.debug))
    }

    fun databaseForType(type: BackendDatabaseType): Databases {
        return when(type) {
            BackendDatabaseType.DATA -> databaseData
            BackendDatabaseType.KEYS -> databaseKeys
        }
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
            throw mapException(e).also {
                when ((it as? UnauthorizedException)?.type) {
                    "general_unauthorized_scope", "user_blocked" -> {
                        // user session is invalid
                        sessionMonitor.updateSessionState(sessionValid = false)
                    }
                    else -> {}
                }
            }
        }
    }

    companion object {
        fun mapException(exception: Exception): Exception {
            return (exception as? AppwriteException)?.let {
                BaseRemoteException.fromCode(
                    code = it.code,
                    message = it.message,
                    cause = it.cause ?: Throwable(it.response),
                    type = it.type,
                )
            } ?: let {
                if (exception is UnknownHostException) {
                    RemoteConnectionException(exception.message, code = null, exception)
                } else exception
            }
        }
    }

}