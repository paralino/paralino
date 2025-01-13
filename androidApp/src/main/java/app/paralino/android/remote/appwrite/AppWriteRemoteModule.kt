package app.paralino.android.remote.appwrite

import android.app.Application
import app.paralino.android.remote.RemoteClient
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.provider.isConnectionError
import app.paralino.core.provider.isSocketTimeout
import app.paralino.core.utils.NetworkQualityMonitor
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.exceptions.RemoteConnectionException
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import io.appwrite.services.Realtime
import kotlinx.coroutines.delay
import java.net.UnknownHostException
import java.util.UUID

class AppWriteRemoteModule(
    private val app: Application,
    val appBuildConfig: AppBuildConfig,
    val sessionMonitor: UserSessionMonitor,
    val networkQualityMonitor: NetworkQualityMonitor
) {

    internal val account: Account by lazy { Account(client) }
    internal val database: Databases by lazy { Databases(client) }
    internal val functions: Functions by lazy { Functions(client) }
    internal val realtime: Realtime by lazy { Realtime(client) }

    private val client: Client by lazy {
        Client(
            context = app,
            endpoint = appBuildConfig.endpoint,
            selfSigned = false
        )
            .setEndpoint(appBuildConfig.endpoint)
            .setProject(appBuildConfig.projectId)
            .run {
                RemoteClient.setup(
                    client = this,
                    logRequests = appBuildConfig.logToConsole
                )
            }
    }

    suspend inline fun <T> execute(forceLogoutOnError: Boolean = false, action: AppWriteRemoteModule.() -> T): T {
        val key = UUID.randomUUID().toString()
        networkQualityMonitor.addEntry(key)
        try {
            val result = try {
                action(this)
            } catch (e: Exception) {
                if (e is UnknownHostException) {
                    delay(200)
                    action(this)
                } else {
                    throw e
                }
            }

            networkQualityMonitor.stopEntry(key = key, errorType = null)

            return result
        } catch (e: Exception) {
            if (e.isSocketTimeout()) {
                networkQualityMonitor.stopEntry(key = key, errorType = NetworkQualityMonitor.NetworkRequestErrorType.TIMEOUT)
            } else if (e.isConnectionError()) {
                networkQualityMonitor.stopEntry(key = key, errorType = NetworkQualityMonitor.NetworkRequestErrorType.UNKNOWN_HOST)
            } else {
                networkQualityMonitor.stopEntry(key = key, errorType = NetworkQualityMonitor.NetworkRequestErrorType.OTHER)
            }

            throw mapException(e, forceLogoutOnError).also {
                sessionMonitor.processNetworkError(it)
            }
        }
    }

    companion object {
        fun mapException(exception: Exception, forceLogoutOnError: Boolean): Exception {
            return (exception as? AppwriteException)?.let {
                BaseRemoteException.fromCode(
                    code = it.code,
                    message = it.message,
                    cause = it.cause ?: Throwable(it.response),
                    // TODO review: using this as temporary workaround for some user api endpoints not parsing correct type
                    // in case when session expires calling user.get() will return 401 but with text/html as response
                    type = it.type ?: if(forceLogoutOnError) UserSessionMonitor.unauthorizedTypes.firstOrNull() else null,
                )
            } ?: let {
                if (exception is UnknownHostException) {
                    RemoteConnectionException(exception.message, code = null, exception)
                } else exception
            }
        }
    }

}