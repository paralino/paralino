package app.paralino.core.models.location

import app.paralino.core.repo.location.LocationFilteringType
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

data class LocationUpdateDataFromDevice(
    val lat: Double,
    val lng: Double,
    val rawHorizontalAccuracy: Float,
    val timeUTC: String?,
    val speed: Float?,
    val altitude: Float?,
    val provider: LocationProviderType?,
    val isLikelyInaccurate: Boolean?,
) {

    val timeUTCInstant: Instant? = timeUTC?.fromUTCtoInstant()

    // Accuracy is in meters at 68% confidence
    // for more inaccurate location we want to have better confidence
    // this will improve perceived accuracy on the map and in further calculations
    val adjustedHorizontalAccuracy: Float = when {
        rawHorizontalAccuracy > 500 -> rawHorizontalAccuracy * 1.47f // Scale to 85% confidence
        rawHorizontalAccuracy > 300 -> {
            // Linear interpolation between 75% (1.15) and 85% (1.47)
            val scale = 1.15f + (1.47f - 1.15f) * (rawHorizontalAccuracy - 300) / (500 - 300)
            rawHorizontalAccuracy * scale.coerceIn(1.15f, 1.47f)
        }
        else -> rawHorizontalAccuracy
    }

    fun isOlderThan(duration: Duration): Boolean {
        val pinTime = timeUTCInstant ?: return false
        return pinTime.plus(duration) < nowUTC()
    }

    fun takeMostRelevantLocation(
        second: LocationUpdateDataFromDevice,
        type: LocationPerformanceSettingsType.Balanced,
        filtering: LocationFilteringType
    ): LocationUpdateDataFromDevice {
        val firstTime = timeUTCInstant
        val secondTime = second.timeUTCInstant
        if (firstTime == null || secondTime == null) {
            return second
        }

        val timeThreshold = when (type) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2 -> 12.seconds
            LocationPerformanceSettingsType.Balanced.Level3 -> 8.seconds
            LocationPerformanceSettingsType.Balanced.Level4,
            LocationPerformanceSettingsType.Balanced.Level5 -> 6.seconds
        }

        val distanceThreshold = when (type) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2 -> 30
            LocationPerformanceSettingsType.Balanced.Level3 -> 20
            LocationPerformanceSettingsType.Balanced.Level4,
            LocationPerformanceSettingsType.Balanced.Level5 -> 10
        }

        val accuracyThreshold = when (type) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2 -> 15f
            LocationPerformanceSettingsType.Balanced.Level3 -> 10f
            LocationPerformanceSettingsType.Balanced.Level4,
            LocationPerformanceSettingsType.Balanced.Level5 -> 5f
        }

        val altitudeThreshold = when (type) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2 -> 30f
            LocationPerformanceSettingsType.Balanced.Level3 -> 20f
            LocationPerformanceSettingsType.Balanced.Level4,
            LocationPerformanceSettingsType.Balanced.Level5 -> 10f
        }

        val speedThresholdKmh = when (type) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2 -> 8f
            LocationPerformanceSettingsType.Balanced.Level3 -> 5f
            LocationPerformanceSettingsType.Balanced.Level4,
            LocationPerformanceSettingsType.Balanced.Level5 -> 3f
        }

        val timeDifference = (firstTime - secondTime).absoluteValue

        // Some devices (mostly GPS only on Android) mix accurate fixes with very inaccurate ones
        // every few seconds, which makes location jump back and forth on the map
        // Degraded update is dropped unless nothing better comes in for a while
        val degradedAccuracyLimit = max(
            adjustedHorizontalAccuracy * filtering.degradedAccuracyRatio,
            adjustedHorizontalAccuracy + filtering.degradedAccuracyMarginMeters
        )
        val isDegraded = second.adjustedHorizontalAccuracy > degradedAccuracyLimit
        if (isDegraded && timeDifference <= timeThreshold * filtering.degradedHoldMultiplier) {
            return this
        }

        val timeValid = timeDifference <= timeThreshold
        val distanceValid = isWithinRange(other = second, thresholdMeters = distanceThreshold)
        val accuracyValid = (adjustedHorizontalAccuracy - second.adjustedHorizontalAccuracy).absoluteValue <= accuracyThreshold

        val speedThresholdMs = speedThresholdKmh / 3.6f
        val speedValid = if (speed != null && second.speed != null) {
            (speed - second.speed).absoluteValue < speedThresholdMs
        } else {
            speed == second.speed
        }

        val altitudeValid = if (altitude != null && second.altitude != null) {
            (altitude - second.altitude).absoluteValue < altitudeThreshold
        } else {
            altitude == second.altitude
        }

        return if (
            timeValid
            && distanceValid
            && accuracyValid
            && speedValid
            && altitudeValid
        ) {
            this
        } else {
            second
        }
    }

    // flat-earth approximation - use for only short distance check
    private fun isWithinRange(
        other: LocationUpdateDataFromDevice,
        thresholdMeters: Int = 10
    ): Boolean {
        val avgLat = (lat + other.lat) / 2
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLng = 111320.0 * cos(avgLat * 0.01745329252)

        val deltaLat = (other.lat - lat) * metersPerDegreeLat
        val deltaLng = (other.lng - lng) * metersPerDegreeLng

        val distanceSquared = deltaLat * deltaLat + deltaLng * deltaLng
        return distanceSquared <= (thresholdMeters * thresholdMeters)
    }


    companion object {
        fun getBetter(first: LocationUpdateDataFromDevice?, second: LocationUpdateDataFromDevice?): LocationUpdateDataFromDevice? {
            if (second?.timeUTCInstant == null) {
                return first
            } else if (first?.timeUTCInstant != null) {
                // if new location is older than currently pending one, discard it
                if (first.timeUTCInstant > second.timeUTCInstant) {
                    // first is newer, second is older
                    val difference = first.timeUTCInstant - second.timeUTCInstant
                    if (difference < 1.seconds && second.adjustedHorizontalAccuracy < first.adjustedHorizontalAccuracy && (first.adjustedHorizontalAccuracy - second.adjustedHorizontalAccuracy).absoluteValue > 30) {
                        // time difference is not huge but accuracy is better, use second location
                        return second
                    } else {
                        return first
                    }
                } else if (second.timeUTCInstant > first.timeUTCInstant) {
                    val difference = second.timeUTCInstant - first.timeUTCInstant
                    if (difference < 1.seconds && first.adjustedHorizontalAccuracy < second.adjustedHorizontalAccuracy && (first.adjustedHorizontalAccuracy - second.adjustedHorizontalAccuracy).absoluteValue > 30) {
                        // time difference is not huge but accuracy is better, use first location
                        return first
                    } else {
                        return second
                    }
                } else {
                    return if (first.adjustedHorizontalAccuracy < second.adjustedHorizontalAccuracy) first else second
                }
            } else {
                return second
            }
        }
    }
}

enum class LocationProviderType constructor(
    val key: String,
    val historyKey: String
) {
    NETWORK(key = "network", historyKey = "n"),
    GPS(key = "gps", historyKey = "g"),
    FUSED(key = "fused", historyKey = "f");

    companion object {
        fun fromFullKey(key: String?): LocationProviderType? {
            return LocationProviderType.entries.firstOrNull { it.key == key }
        }

        fun fromHistoryKey(historyKey: String?): LocationProviderType? {
            return LocationProviderType.entries.firstOrNull { it.historyKey == historyKey }
        }
    }
}