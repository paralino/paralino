package com.zbapp.locationtrack.utils.extensions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun LocalDateTime.toMillis(): Long? {
    return toInstant(ZoneOffset.UTC)?.toEpochMilli()
}

fun Long.toDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}