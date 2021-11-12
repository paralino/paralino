package com.zbapp.locationtrack.utils.extensions

import com.zbapp.core.models.Payload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

inline fun <reified T, R> Flow<T>.mapToPayload(
    throwError: Boolean = false,
    noinline transform: suspend (value: T) -> R
): Flow<Payload<R>> {
    return this
        .map<T, Payload<R>> { Payload.Success(transform.invoke(it)) }
        .onStart { emit(Payload.Loading) }
        .catch {
            emit(Payload.Error(it))
            if (throwError) throw it
        }
}