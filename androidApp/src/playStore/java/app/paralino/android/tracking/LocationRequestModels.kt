package app.paralino.android.tracking

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

object LocationRequestModels {

    fun getRequest(
        type: LocationPerformanceType,
        balancedSettings: LocationPerformanceManager.LocationPerformanceSettings,
        batterySaverSettings: LocationPerformanceManager.LocationPerformanceSettings,
    ): LocationRequest {
        return when (type) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.VERY_HIGH,
            LocationPerformanceType.MANUAL_BOOST -> {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .build()
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 60 * 1000L)
                    .setMinUpdateIntervalMillis(10_000L)
                    .build()
            }

            LocationPerformanceType.BALANCED -> {
                LocationRequest.Builder(
                    if (balancedSettings.useHighPerformanceLocation) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    balancedSettings.updateIntervalMillis
                )
                    .setMinUpdateIntervalMillis(balancedSettings.minUpdateIntervalMillis)
                    .setMinUpdateDistanceMeters(balancedSettings.minUpdateDistanceMeters.toFloat())
                    .build()
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    batterySaverSettings.updateIntervalMillis
                )
                    .setMinUpdateIntervalMillis(batterySaverSettings.minUpdateIntervalMillis)
                    .setMinUpdateDistanceMeters(batterySaverSettings.minUpdateDistanceMeters.toFloat())
                    .build()
            }

            LocationPerformanceType.HIGH_BATTERY_SAVER -> {
                val settings = LocationPerformanceSettingsType.BatterySaver.Level1
                LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    settings.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(settings.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(settings.minUpdateDistanceMeters.toFloat())
                    .build()
            }
        }
    }

    fun getRequest(
        type: LocationPerformanceType,
        appPrefs: AppPrefs
    ): LocationRequest {
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