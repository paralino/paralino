package com.zbapp.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

fun <T> MutableStateFlow<T>.replace(action: T.() -> T) {
    value = action(value)
}

suspend inline fun <T> blocking(noinline action: suspend CoroutineScope.() -> T) = withContext(
    Dispatchers.IO, action)