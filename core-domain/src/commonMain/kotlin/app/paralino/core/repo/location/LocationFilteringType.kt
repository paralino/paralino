package app.paralino.core.repo.location

enum class LocationFilteringType(
    val key: String,
    val degradedAccuracyRatio: Float,
    val degradedAccuracyMarginMeters: Float,
    val degradedHoldMultiplier: Int
) {
    Low(
        key = "filtering_low",
        degradedAccuracyRatio = 3f,
        degradedAccuracyMarginMeters = 60f,
        degradedHoldMultiplier = 1
    ),
    Balanced(
        key = "filtering_balanced",
        degradedAccuracyRatio = 2f,
        degradedAccuracyMarginMeters = 30f,
        degradedHoldMultiplier = 2
    ),
    High(
        key = "filtering_high",
        degradedAccuracyRatio = 1.5f,
        degradedAccuracyMarginMeters = 15f,
        degradedHoldMultiplier = 4
    ),
    VeryHigh(
        key = "filtering_very_high",
        degradedAccuracyRatio = 1.3f,
        degradedAccuracyMarginMeters = 12f,
        degradedHoldMultiplier = 6
    );

    companion object {
        val default: LocationFilteringType = Balanced

        fun fromKey(key: String?): LocationFilteringType {
            return entries.firstOrNull { it.key == key } ?: default
        }
    }
}
