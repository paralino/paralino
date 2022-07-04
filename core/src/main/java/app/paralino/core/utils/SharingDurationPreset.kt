package app.paralino.core.utils

import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toTimeUnit

@OptIn(ExperimentalTime::class)
object SharingDurationPreset {

    private const val second = 1_000L
    private const val minute = 60 * second
    const val hour = 60 * minute
    const val day = 24 * hour

    const val backgroundAccessThreshold = 2 * day
    val publicSharingMaxDurationMillis: Long = DurationUnit.HOURS.toTimeUnit().toMillis(5)

    val presets = listOf(
        15 * minute,
        30 * minute,
        hour,
        2 * hour,
        4 * hour,
        8 * hour,
        12 * hour,
        day,
        2 * day,
        3 * day,
        5 * day,
        7 * day,
        10 * day,
        15 * day,
        30 * day,
        365 * day,
        0
    )

}