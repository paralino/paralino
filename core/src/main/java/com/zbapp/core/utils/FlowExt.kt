package com.zbapp.core.utils

import com.zbapp.core.models.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

fun <T> MutableStateFlow<T>.replace(action: T.() -> T) {
    value = action(value)
}

suspend inline fun <T> blocking(noinline action: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, action)
}

inline fun <reified T, R> Flow<T>.mapToPayload(
    throwError: Boolean = false,
    noinline transform: suspend (value: T) -> R = { it as R }
): Flow<Payload<R>> {
    return this
        .map<T, Payload<R>> { Payload.Success(transform.invoke(it)) }
        .onStart { emit(Payload.Loading) }
        .catch {
            emit(Payload.Error(it))
            if (throwError) throw it
        }
}

inline fun <reified DATA> Flow<List<DATA>>.distinctFlowOfList(): Flow<List<DATA>> {
    return this.map { it.toTypedArray() }
        .distinctUntilChanged { old, new ->
            old.contentEquals(new)
        }.map { it.toList() }
}