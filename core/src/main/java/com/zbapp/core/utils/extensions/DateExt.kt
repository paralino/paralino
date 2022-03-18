package com.zbapp.core.utils.extensions

import kotlinx.datetime.*

fun nowUTC(): Instant = Clock.System.now()

fun String.isoToLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(isoString = this)
    } catch (e: Exception) {
        null
    }
}

fun String.fromUTCtoInstant(): Instant? {
    return isoToLocalDateTime()?.toInstant(TimeZone.UTC)
}