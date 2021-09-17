package com.zbapp.crpyto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun <T> blocking(noinline action: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, action)
}