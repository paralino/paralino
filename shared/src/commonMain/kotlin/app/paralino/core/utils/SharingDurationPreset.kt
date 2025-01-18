package app.paralino.core.utils

object SharingDurationPreset {

    private const val second = 1_000L
    private const val minute = 60 * second
    const val hour = 60 * minute
    const val day = 24 * hour
    const val infinite = -1L

    const val backgroundAccessThreshold = 8 * hour
    val publicSharingMaxDurationMillis: Long = 5 * hour

    val premiumPresets = listOf(
        15 * minute,
        hour,
        4 * hour,
        8 * hour,
        day,
        2 * day,
        3 * day,
        5 * day,
        7 * day,
        10 * day,
        15 * day,
        20 * day,
        30 * day,
        365 * day,
        infinite
    )

    val freePresets = listOf(
        15 * minute,
        hour,
        4 * hour,
        8 * hour,
        day
    )

    fun presets(isPremium: Boolean): List<Long> {
        return if (isPremium) premiumPresets else freePresets
    }
}