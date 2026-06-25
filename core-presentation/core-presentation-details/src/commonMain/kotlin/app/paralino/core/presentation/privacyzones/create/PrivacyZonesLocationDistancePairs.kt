package app.paralino.core.presentation.privacyzones.create

data class PrivacyZoneLocationDistancePair(
    val meters: Int,
    val metersForImperial: Double,
) {
    companion object {
        private const val FEET_PER_METER = 3.28084
        private const val METERS_PER_MILE = 1_609.344

        val default = pair(meters = 500, miles = 0.3)

        val pairs = listOf(
            pair(meters = 100, feet = 325),
            pair(meters = 150, feet = 500),
            pair(meters = 200, feet = 650),
            pair(meters = 300, feet = 1_000),
            pair(meters = 400, miles = 0.25),
            pair(meters = 500, miles = 0.3),
            pair(meters = 600, miles = 0.35),
            pair(meters = 700, miles = 0.4),
            pair(meters = 800, miles = 0.5),
            pair(meters = 900, miles = 0.55),
            pair(meters = 1_000, miles = 0.6),
            pair(meters = 1_200, miles = 0.75),
            pair(meters = 1_500, miles = 0.9),
            pair(meters = 1_800, miles = 1.1),
            pair(meters = 2_000, miles = 1.25),
            pair(meters = 2_200, miles = 1.4),
            pair(meters = 2_500, miles = 1.55),
            pair(meters = 2_800, miles = 1.75),
            pair(meters = 3_000, miles = 1.9),
            pair(meters = 3_500, miles = 2.2),
            pair(meters = 4_000, miles = 2.5),
            pair(meters = 4_500, miles = 2.8),
            pair(meters = 5_000, miles = 3.1),
            pair(meters = 6_000, miles = 3.7),
            pair(meters = 7_000, miles = 4.35),
            pair(meters = 8_000, miles = 5.0),
            pair(meters = 9_000, miles = 5.6),
            pair(meters = 10_000, miles = 6.2),
            pair(meters = 12_500, miles = 7.8),
            pair(meters = 15_000, miles = 9.3),
            pair(meters = 17_500, miles = 10.9),
            pair(meters = 20_000, miles = 12.5),
        )

        fun pairForSelectedMeters(meters: Int): PrivacyZoneLocationDistancePair? {
            return pairs.firstOrNull { it.meters == meters }
                ?: pairs.firstOrNull { meters < it.meters }
        }

        private fun pair(meters: Int, feet: Int): PrivacyZoneLocationDistancePair {
            return PrivacyZoneLocationDistancePair(
                meters = meters,
                metersForImperial = feet / FEET_PER_METER,
            )
        }

        private fun pair(meters: Int, miles: Double): PrivacyZoneLocationDistancePair {
            return PrivacyZoneLocationDistancePair(
                meters = meters,
                metersForImperial = miles * METERS_PER_MILE,
            )
        }
    }
}
