package app.paralino.core.utils.extensions

import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration

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

fun String.isoToLocalDateTimeAtTimezone(timezone: TimeZone): LocalDateTime? {
    try {
        return isoToLocalDateTime()
            ?.toInstant(TimeZone.UTC)
            ?.toLocalDateTime(timezone)
    } catch (e: Exception) {
        return null
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
        Instant.parse(input = this)
    } catch (e: Exception) {
        null
    }
}

fun Instant.isInPast(): Boolean {
    return this < nowUTC()
}

fun Instant.isInFuture(): Boolean {
    return this > nowUTC()
}

fun Instant.isOlderThan(duration: Duration): Boolean {
    return this.plus(duration) < nowUTC()
}

fun LocalDateTime.stripNanoseconds(): LocalDateTime {
    return LocalDateTime(
        date = this.date,
        time = LocalTime(
            hour = this.time.hour,
            minute = this.time.minute,
            second = this.time.second,
            nanosecond = 0
        )
    )
}

fun LocalDateTime.stripSeconds(): LocalDateTime {
    return LocalDateTime(
        date = this.date,
        time = LocalTime(
            hour = this.time.hour,
            minute = this.time.minute,
            second = 0,
            nanosecond = 0
        )
    )
}

fun LocalDate.historyDateKeyForApi(): String {
    return this.format(LocalDate.Format { byUnicodePattern("yyyy-MM-dd") })
}

fun String.historyDateKeyFromApi(): LocalDate? {
    return try {
        LocalDate.parse(input = this, format = LocalDate.Formats.ISO)
    } catch (e: Exception) {
        null
    }
}

fun LocalDate.listOfDatesUntil(date: LocalDate): List<LocalDate> {
    var currentDate = this
    val dateList = mutableListOf<LocalDate>()
    while (currentDate <= date) {
        dateList.add(currentDate)
        currentDate = currentDate.plus(DatePeriod(days = 1))
    }
    return dateList
}