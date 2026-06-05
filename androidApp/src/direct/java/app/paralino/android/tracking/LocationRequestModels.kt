package app.paralino.android.tracking

import androidx.core.location.LocationRequestCompat
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType

object LocationRequestModels {

    fun getRequest(
        type: LocationPerformanceType,
        balancedSettings: LocationPerformanceManager.LocationPerformanceSettings,
        batterySaverSettings: LocationPerformanceManager.LocationPerformanceSettings,
    ): LocationRequestCompat {
        return when (type) {
            LocationPerformanceType.HIGHER_FOCUS,
            LocationPerformanceType.VERY_HIGH,
            LocationPerformanceType.MANUAL_BOOST -> {
                LocationRequestCompat.Builder(1_000L)
                    .setMinUpdateIntervalMillis(1_000L)
                    .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                    .build()
            }

            LocationPerformanceType.HIGH -> {
                LocationRequestCompat.Builder(3_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                    .build()
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                LocationRequestCompat.Builder(5 * 60 * 1000L)
                    .setMinUpdateIntervalMillis(10_000L)
                    .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                    .build()
            }

            LocationPerformanceType.BALANCED -> {
                LocationRequestCompat.Builder(
                    balancedSettings.updateIntervalMillis
                )
                    .setMinUpdateIntervalMillis(balancedSettings.minUpdateIntervalMillis)
                    .setMinUpdateDistanceMeters(balancedSettings.minUpdateDistanceMeters.toFloat())
                    .setQuality(if (balancedSettings.useHighPerformanceLocation) LocationRequestCompat.QUALITY_HIGH_ACCURACY else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                    .build()
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                LocationRequestCompat.Builder(
                    batterySaverSettings.updateIntervalMillis
                )
                    .setMinUpdateIntervalMillis(batterySaverSettings.minUpdateIntervalMillis)
                    .setMinUpdateDistanceMeters(batterySaverSettings.minUpdateDistanceMeters.toFloat())
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

    fun getRequest(
        type: LocationPerformanceType,
        appPrefs: AppPrefs
    ): LocationRequestCompat {
        return getRequest(
            type = type,
            balancedSettings = LocationPerformanceManager.LocationPerformanceSettings(
                settingsType = LocationPerformanceSettingsType.Balanced.fromKey(appPrefs.locationSettingsBalancedPerformanceType),
                useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy
            ),
            batterySaverSettings = LocationPerformanceManager.LocationPerformanceSettings(
                settingsType = LocationPerformanceSettingsType.BatterySaver.fromKey(appPrefs.locationSettingsBatterySaverPerformanceType),
                useHighPerformanceLocation = false
            )
        )
    }
}