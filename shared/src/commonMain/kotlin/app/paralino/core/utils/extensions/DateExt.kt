package app.paralino.core.utils.extensions

import kotlinx.datetime.*

fun nowUTC(): Instant = Clock.System.now()

fun String.isoToLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(isoString = this)
    } catch (e: Exception) {
        try {
            LocalDate.parse(isoString = this).atTime(0, 0)
        } catch (e: Exception) {
            null
        }
    }
}

fun String.fromUTCtoInstant(): Instant? {
    return try {
        isoToLocalDateTime()?.toInstant(TimeZone.UTC)
    } catch (e: Exception) {
        null
    }
}

fun String.isoWithOffsetToInstant(): Instant? {
    return try {
        Instant.parse(isoString = this)
    } catch (e: Exception) {
        null
    }
}