package app.paralino.core.repo.base

import app.paralino.core.connectionStatus
import app.paralino.core.logg
import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.*


abstract class BaseRepository {

    private val connection get() = connectionStatus
    private val cachedInfo = hashMapOf<String, Long>()

    /**
     * Will subscribe to local data flow, and also refresh the data from remote
     *
     * @param local - local data flow
     * @param remote - remote data flow
     * @param localUpdate - function invoked when data is updated from remote
     * @param remotePreference - flag indicating how execution will use remote data
     */
    protected fun <T, U> mergeDataWithRemote(
        local: Flow<T>,
        localUpdate: (suspend (U) -> Unit),
        remotePreference: RemotePreference = RemotePreference.PREFER,
        remote: (suspend () -> U)
    ): Flow<T> {
        return local.onStart {
            val localValue = local.firstOrNull()?.also { emit(it) }
            triggerRemote(localValue, remotePreference, remote)?.let {
                localUpdate.invoke(it)
            }
        }
            // TODO check debounce issue
            // due to some bug, when local data is updated from remote it is emitted twice
            // so we just ignore values if they are published way to fast
            // find a better solution or maybe it will be fixed when FlowAPI reaches stable
            .debounce(10)
            .catch {
                logg.e(it)
                throw it
            }
    }


    /**
     * Get data from local data flow if available, and refresh the data from remote
     *
     * @param local - local data flow, optional
     * @param remote - remote data flow
     * @param localUpdate - function invoked when data is updated from remote, optional
     * @param remotePreference - flag indicating how execution will use remote data
     */
    protected fun <T> getDataOnce(
        local: Flow<T>? = null,
        localUpdate: (suspend (T) -> Unit)? = null,
        remotePreference: RemotePreference = RemotePreference.FORCE,
        remote: (suspend () -> T)
    ): Flow<T> {
        return flow<T> {
            val localValue = local?.firstOrNull()?.also { emit(it) }
            triggerRemote(localValue, remotePreference, remote)?.let {
                localUpdate?.invoke(it)
                emit(it)
            }
        }.catch {
            logg.e(it)
            throw it
        }
    }

    suspend fun <T> triggerRemoteOnly(
        localValue: T? = null,
        remote: (suspend () -> T)
    ): T {
        return try {
            triggerRemote(
                localValue = localValue,
                remotePreference = RemotePreference.FORCE,
                remote = remote
            )!!
        } catch (e: Exception) {
            logg.e(e)
            throw e
        }
    }

    private suspend fun <T, U> triggerRemote(
        localValue: T? = null,
        remotePreference: RemotePreference,
        remote: (suspend () -> U)
    ): U? {

        val isOnline = connection.initAndGetStatus().isOnline

        return when(remotePreference) {
            RemotePreference.FORCE -> {
                if(!isOnline)
                    throwOfflineError()
                remote.invoke()
            }
            RemotePreference.PREFER -> {
                when {
                    isOnline -> remote.invoke()
                    localValue == null -> throwOfflineError()
                    else -> null
                }
            }
            RemotePreference.IGNORE -> {
                if(localValue == null) {
                    if(!isOnline)
                        throwOfflineError()
                    remote.invoke()
                } else null
            }
            is RemotePreference.CACHE -> {
                val started = cachedInfo[remotePreference.key] ?: 0
                val now = nowUTC().toEpochMilliseconds()
                val cacheIsDirty = now - started > remotePreference.timeMillis

                val remoteData = when {
                    localValue == null -> {
                        if (!isOnline) throwOfflineError()
                        cachedInfo[remotePreference.key] = now
                        remote.invoke()
                    }
                    cacheIsDirty -> {
                        logg.tag("refresh_cache").v("Cache is dirty for ${remotePreference.key} - ${now - started} > ${remotePreference.timeMillis}")
                        when {
                            remotePreference.failOnError && !isOnline -> throwOfflineError()
                            isOnline -> {
                                cachedInfo[remotePreference.key] = now
                                remote.invoke()
                            }
                            else -> {
                                cachedInfo[remotePreference.key] = 0
                                null
                            }
                        }
                    }
                    else -> {
                        logg.tag("refresh_cache").v("Cache not dirty for ${remotePreference.key} - ${now - started} > ${remotePreference.timeMillis}")
                        null
                    }
                }

                remoteData?.also {
                    cachedInfo[remotePreference.key] = nowUTC().toEpochMilliseconds()
                }
            }
        }
    }

    private fun throwOfflineError(): Nothing {
        throw InternalConnectionException(connection.status, "Device is offline: ${connection.status}")
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        return try {
            first()
        } catch (e: Exception) {
            null
        }
    }

}

sealed class RemotePreference {

    /** try to use remote, fail if remote not available */
    object FORCE: RemotePreference()

    /** try to use remote, will not fail if remote not available */
    object PREFER: RemotePreference()

    /** do not use remote if local data is available */
    object IGNORE: RemotePreference()

    data class CACHE(val key: String, val timeMillis: Long, val failOnError: Boolean = false): RemotePreference()
}