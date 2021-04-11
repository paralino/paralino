package com.zbapp.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun LocalDateTime.toUTCString(): String {
    return this.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

suspend inline fun <T> blocking(noinline action: suspend CoroutineScope.() -> T) = withContext(Dispatchers.IO, action)