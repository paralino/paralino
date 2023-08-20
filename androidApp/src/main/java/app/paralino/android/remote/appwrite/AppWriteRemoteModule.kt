package app.paralino.android.remote.appwrite

import android.app.Application
import app.paralino.android.remote.RemoteClient
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.RemoteConnectionException
import app.paralino.core.utils.exceptions.UnauthorizedException
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import io.appwrite.services.Realtime
import java.net.UnknownHostException

class AppWriteRemoteModule(
    private val app: Application,
    val backendConfig: BackendConfig,
    val sessionMonitor: UserSessionMonitor,
) {

    internal val account: Account by lazy { Account(client) }
    internal val database: Databases by lazy { Databases(client) }
    internal val functions: Functions by lazy { Functions(client) }
    internal val realtime: Realtime by lazy { Realtime(client) }

    private val client: Client by lazy {
        Client(
            context = app,
            endPoint = backendConfig.endpoint,
            selfSigned = false
        )
            .setEndpoint(backendConfig.endpoint)
            .setProject(backendConfig.projectId)
            .run { RemoteClient.setup(this, backendConfig.debug) }
    }

    inline fun <T> execute(action: AppWriteRemoteModule.() -> T): T {
        try {
            return action(this)
        } catch (e: Exception) {
            throw mapException(e).also {
                sessionMonitor.processNetworkError(it)
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