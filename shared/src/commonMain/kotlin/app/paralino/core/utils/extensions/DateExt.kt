package app.paralino.core.utils.extensions

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents

fun nowUTC(): Instant = Clock.System.now()

fun String.isoToLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(input = this, format = LocalDateTime.Formats.ISO)
    } catch (e: Exception) {
        try {
            LocalDate.parse(input = this, format = LocalDate.Formats.ISO)
                .atTime(hour = 0, minute = 0)
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
        Instant.parse(input = this, format = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
    } catch (e: Exception) {
        null
    }
}