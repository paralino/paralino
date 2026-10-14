package app.paralino.android.tracking

import androidx.core.location.LocationRequestCompat
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import kotlin.time.Duration.Companion.minutes

object LocationRequestModels {

    fun getRequest(
        type: LocationPerformanceType,
        balancedSettings: LocationPerformanceManager.LocationPerformanceSettingsBalanced,
        batterySaverSettings: LocationPerformanceManager.LocationPerformanceSettingsBatterySaver,
    ): LocationRequestCompat {
        return when (type) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.HIGHER_FOCUS,
            LocationPerformanceType.VERY_HIGH,
            LocationPerformanceType.MANUAL_BOOST -> {
                LocationRequestCompat.Builder(1_000L)
                    .setMinUpdateIntervalMillis(1_000L)
                    .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                    .build()
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                LocationRequestCompat.Builder(1.minutes.inWholeMilliseconds)
                    .setMinUpdateIntervalMillis(10_000L)
                    .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                    .build()
            }

            LocationPerformanceType.BALANCED -> {
                LocationRequestCompat.Builder(
                    balancedSettings.config.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(balancedSettings.config.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(balancedSettings.config.minUpdateDistanceMeters.toFloat())
                    .setQuality(if (balancedSettings.useHighPerformanceLocation) LocationRequestCompat.QUALITY_HIGH_ACCURACY else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                    .build()
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                LocationRequestCompat.Builder(
                    batterySaverSettings.config.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(batterySaverSettings.config.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(batterySaverSettings.config.minUpdateDistanceMeters.toFloat())
                    .setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                    .build()
            }

            LocationPerformanceType.HIGH_BATTERY_SAVER -> {
                val settings = LocationPerformanceSettingsType.BatterySaver.Level1
                LocationRequestCompat.Builder(
                    settings.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(settings.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(settings.minUpdateDistanceMeters.toFloat())
                    .setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                    .build()
            }
        }
    }
}