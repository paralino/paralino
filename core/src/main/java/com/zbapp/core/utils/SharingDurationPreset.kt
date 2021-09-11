package com.zbapp.core.utils

import kotlin.math.min

object SharingDurationPreset {

    private const val second = 1_000L
    private const val minute = 60 * second
    private const val hour = 60 * minute
    private const val day = 24 * hour

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