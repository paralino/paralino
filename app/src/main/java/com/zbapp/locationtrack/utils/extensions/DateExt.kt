package com.zbapp.locationtrack.utils.extensions

import java.time.*
import java.time.format.DateTimeFormatter

fun LocalDateTime.toMillis(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.toDateTime(): ZonedDateTime {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
}

fun Long.toDateTimeUTC(): LocalDateTime {
    return toDateTime().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
}

fun String.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
}

fun LocalDateTime.toLocalString(): String {
    return this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

fun LocalDateTime.toUTCString(): String {
    return this.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}