package com.zbapp.locationtrack.utils.extensions

import android.text.format.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

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

fun LocalDateTime.fromUTCtoLocal(): LocalDateTime {
    return LocalDateTime.ofInstant(this.toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
}

fun LocalDateTime.fromLocalToUTC(): LocalDateTime {
    return this.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
}

fun LocalDateTime.fromUTCtoMapSharingTime(): String? {
    val local = this.fromUTCtoLocal()
    val now = LocalDateTime.now()
    val sameYear = local.year == now.year
    val sameDay = sameYear && local.dayOfYear == now.dayOfYear
    val time = local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return when {
        sameDay -> time
        sameYear -> local.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"))) + ", $time"
        else -> local.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdyyyy"))) + "\n$time"
    }
}