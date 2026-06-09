package app.paralino.core.presentation.share

data class ApproximateLocationDistancePair(
    val meters: Int,
    val metersForImperial: Double
) {
    companion object {
        val default = ApproximateLocationDistancePair(meters = 2_000, metersForImperial = 1_931.0)
        val pairs = listOf(
            ApproximateLocationDistancePair(meters = 500, metersForImperial = 483.0),
            ApproximateLocationDistancePair(meters = 1_000, metersForImperial = 965.0),
            ApproximateLocationDistancePair(meters = 2_000, metersForImperial = 1_931.0),
            ApproximateLocationDistancePair(meters = 5_000, metersForImperial = 4_830.0),
            ApproximateLocationDistancePair(meters = 10_000, metersForImperial = 9_656.0),
            ApproximateLocationDistancePair(meters = 15_000, metersForImperial = 14_484.0),
            ApproximateLocationDistancePair(meters = 20_000, metersForImperial = 19_312.0),
        )

        fun pairForSelectedMeters(meters: Int): ApproximateLocationDistancePair? {
            return pairs.firstOrNull { it.meters == meters }
                ?: pairs.firstOrNull { meters < it.meters }
        }
    }
}