package app.paralino.core.repo.location

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class LocationFilteringType(
    val key: String,
    val degradedAccuracyRatio: Float,
    val degradedAccuracyMarginMeters: Float,
    val degradedHoldMultiplier: Int,
    val outsideKnownWifiConfirmFixes: Int,
    val outsideKnownWifiConfirmMinSpan: Duration,
) {
    Low(
        key = "filtering_low",
        degradedAccuracyRatio = 3f,
        degradedAccuracyMarginMeters = 60f,
        degradedHoldMultiplier = 1,
        outsideKnownWifiConfirmFixes = 1,
        outsideKnownWifiConfirmMinSpan = Duration.ZERO
    ),
    Balanced(
        key = "filtering_balanced",
        degradedAccuracyRatio = 2f,
        degradedAccuracyMarginMeters = 30f,
        degradedHoldMultiplier = 2,
        outsideKnownWifiConfirmFixes = 4,
        outsideKnownWifiConfirmMinSpan = 2.minutes
    ),
    High(
        key = "filtering_high",
        degradedAccuracyRatio = 1.5f,
        degradedAccuracyMarginMeters = 15f,
        degradedHoldMultiplier = 4,
        outsideKnownWifiConfirmFixes = 8,
        outsideKnownWifiConfirmMinSpan = 5.minutes
    ),
    VeryHigh(
        key = "filtering_very_high",
        degradedAccuracyRatio = 1.3f,
        degradedAccuracyMarginMeters = 12f,
        degradedHoldMultiplier = 6,
        outsideKnownWifiConfirmFixes = 12,
        outsideKnownWifiConfirmMinSpan = 10.minutes
    );

    companion object {
        val default: LocationFilteringType = Balanced

        fun fromKey(key: String?): LocationFilteringType {
            return entries.firstOrNull { it.key == key } ?: default
        }
    }
}
