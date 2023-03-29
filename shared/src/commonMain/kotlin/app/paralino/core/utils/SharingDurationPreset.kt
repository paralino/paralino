package app.paralino.core.utils

object SharingDurationPreset {

    private const val second = 1_000L
    private const val minute = 60 * second
    const val hour = 60 * minute
    const val day = 24 * hour
    const val infinite = 0L

    const val backgroundAccessThreshold = 2 * day
    val publicSharingMaxDurationMillis: Long = 5 * hour

    val presets = listOf(
        15 * minute,
        hour,
        4 * hour,
        8 * hour,
        day,
        3 * day,
        5 * day,
        10 * day,
        15 * day,
        30 * day,
        365 * day,
        infinite
    )

}