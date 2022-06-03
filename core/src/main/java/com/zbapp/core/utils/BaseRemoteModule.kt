package com.zbapp.core.utils

import com.zbapp.core.utils.exceptions.TimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

object BaseRemoteModule {
    val formatter = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}

suspend inline fun <reified T> poll(
    firstDelayMillis: Long = 2_000,
    delay: Long = 3_000,
    timeout: Long = 15_000,
    canThrow: Boolean = false,
    crossinline action: suspend () -> T?
): T {
    return withTimeoutOrNull(timeout) {
        delay(firstDelayMillis)
        var data: T?
        do {
            data = try {
                action()
            } catch (e: Exception) {
                if (canThrow) throw e
                else null
            }
            if (data == null)
                delay(delay)
        } while (data == null)
        data
    } ?: throw TimeoutException("Invite code request timed out")
}