package com.zbapp.remote.appwrite

import android.app.Application
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.utils.BaseRemoteException
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import io.appwrite.services.Database
import io.appwrite.services.Teams

class AppWriteRemoteModule(
    private val app: Application,
    config: BackendConfig
) {

    internal val account: Account by lazy { Account(client) }
    internal val teams: Teams by lazy { Teams(client) }
    internal val database: Database by lazy { Database(client) }

    private val client: Client by lazy {
        Client(app)
            .setEndpoint(config.serverUrl)
            .setProject(config.projectId)
            .setSelfSigned(config.debug)
    }

    inline fun <T> execute(action: AppWriteRemoteModule.() -> T): T {
        try {
            return action()
        } catch (e: Exception) {
            throw  mapException(e)
        }
    }

    fun mapException(exception: Exception): Exception {
        val sourceException = (exception as? AppwriteException) ?: return exception
        return BaseRemoteException.fromCode(sourceException.code, sourceException.message)
    }

}