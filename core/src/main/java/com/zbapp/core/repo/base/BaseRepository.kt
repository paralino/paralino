package com.zbapp.core.repo.base

import com.zbapp.core.logg
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*


abstract class BaseRepository() {

    open fun isOnline(): Boolean? = true
    private val isOnline: Boolean get() = isOnline() == true

    private val cachedInfo = hashMapOf<String, Long>()

    /**
     * Will subscribe to local data flow, and also refresh the data from remote if available
     *
     * @param local - local data flow
     * @param remote - remote data flow, optional
     * @param localUpdate - function invoked when data is updated from remote, optional
     * @param remotePreference - flag indicating how execution will use remote data
     * @param delay - optional milliseconds delay for remote data, can be used to simulate slow response
     */
    protected fun <T, U> subscribeToData(local: Flow<T>,
                                      remote: (suspend () -> U)? = null,
                                      localUpdate: (suspend (U) -> Unit)? = null,
                                      remotePreference: RemotePreference = RemotePreference.PREFER,
                                      delay: Long? = null): Flow<T> {

        return local.onStart {

            val localValue = local.firstOrNull()
            val remoteValid = remote != null && isOnline

            val cacheState = if(remotePreference is RemotePreference.CACHE) {
                val started = cachedInfo[remotePreference.key] ?: 0
                System.currentTimeMillis() - started > remotePreference.timeMillis
            } else false

            val startRemote = remotePreference is RemotePreference.FORCE || remotePreference is RemotePreference.PREFER || localValue == null || cacheState


            // emit local data first before remote
            if (localValue != null) {
                emit(localValue)
            }

            if (remoteValid && startRemote) {
                // remote is available
                try {
                    remote!!.invoke().let {
                        if(remotePreference is RemotePreference.CACHE) {
                            cachedInfo[remotePreference.key] = System.currentTimeMillis()
                        }
                        if (delay != null) delay(delay)
                        // local update will invoke local flow, so we don't have to emit manually
                        localUpdate?.invoke(it)
                    }
                } catch (e: Exception) {
                    throw e
                }
            } else  {
                // remote is not available
                if (localValue != null) {
                    if(remotePreference is RemotePreference.FORCE || ((remotePreference as? RemotePreference.CACHE)?.failOnError == true && cacheState))
                        throw RemoteNotAvailableException("online: $isOnline, valid: $remoteValid, start: $startRemote")
                } else {
                    error("Local and remote data unavailable")
                }
            }
        }
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
     * Get data from local data flow, and also refresh the data from remote if available
     *
     * @param local - local data flow, optional
     * @param remote - remote data flow, optional
     * @param localUpdate - function invoked when data is updated from remote, optional
     * @param remotePreference - flag indicating how execution will user remote data
     * @param delay - optional milliseconds delay for remote data, can be used to simulate slow response
     */
    protected fun <T> getDataOnce(local: Flow<T>? = null,
                                  remote: (suspend () -> T)? = null,
                                  localUpdate: (suspend (T) -> Unit)? = null,
                                  remotePreference: RemotePreference = RemotePreference.FORCE,
                                  delay: Long? = null): Flow<T> {

        return flow<T>{

            if (local == null && remote == null) {
                error("No data source available")
            }

            val localValue = local?.firstOrNull()

            if (localValue != null) {
                emit(localValue)
            }

            val remoteValid = remote != null && isOnline

            val cacheState = if(remotePreference is RemotePreference.CACHE) {
                val started = cachedInfo[remotePreference.key] ?: 0
                System.currentTimeMillis() - started > remotePreference.timeMillis
            } else false

            val startRemote = remotePreference is RemotePreference.FORCE || remotePreference is RemotePreference.PREFER || localValue == null || cacheState

            if (remoteValid && startRemote) {
                // remote is available
                try {
                    remote!!.invoke().let {

                        if(remotePreference is RemotePreference.CACHE) {
                            cachedInfo[remotePreference.key] = System.currentTimeMillis()
                        }

                        if (delay != null) delay(delay)
                        localUpdate?.invoke(it)
                        emit(it)
                    }
                } catch (e: Exception) {
                    throw e
                }
            } else {
                // remote is not available
                if (localValue != null) {
                    if(remotePreference is RemotePreference.FORCE || ((remotePreference as? RemotePreference.CACHE)?.failOnError == true && cacheState))
                        throw RemoteNotAvailableException("online: $isOnline, valid: $remoteValid, start: $startRemote")
                } else {
                    error("Local and remote data unavailable")
                }
            }

        }.catch {
            logg.e(it)
            throw it
        }
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        return try {
            first()
        } catch (e: Exception) {
            null
        }
    }

}

class RemoteNotAvailableException(val status: String) : Exception("Remote is not available, status: $status")
class NetworkNotAvailableException() : Exception()

sealed class RemotePreference {

    /** try to use remote, fail if remote not available */
    object FORCE: RemotePreference()

    /** try to use remote, will not fail if remote not available */
    object PREFER: RemotePreference()

    /** do not use remote if local data is available */
    object IGNORE: RemotePreference()

    data class CACHE(val key: String, val timeMillis: Long, val failOnError: Boolean = false): RemotePreference()
}