package app.paralino.android.tracking

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import kotlin.time.Duration.Companion.minutes

object LocationRequestModels {

    fun getRequest(
        type: LocationPerformanceType,
        balancedSettings: LocationPerformanceManager.LocationPerformanceSettingsBalanced,
        batterySaverSettings: LocationPerformanceManager.LocationPerformanceSettingsBatterySaver,
    ): LocationRequest {
        return when (type) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.HIGHER_FOCUS,
            LocationPerformanceType.VERY_HIGH,
            LocationPerformanceType.MANUAL_BOOST -> {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                    .setMinUpdateIntervalMillis(1_000L)
                    .build()
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1.minutes.inWholeMilliseconds)
                    .setMinUpdateIntervalMillis(10_000L)
                    .build()
            }

            LocationPerformanceType.BALANCED -> {
                LocationRequest.Builder(
                    if (balancedSettings.useHighPerformanceLocation) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    balancedSettings.config.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(balancedSettings.config.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(balancedSettings.config.minUpdateDistanceMeters.toFloat())
                    .build()
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    batterySaverSettings.config.updateInterval.inWholeMilliseconds
                )
                    .setMinUpdateIntervalMillis(batterySaverSettings.config.minUpdateInterval.inWholeMilliseconds)
                    .setMinUpdateDistanceMeters(batterySaverSettings.config.minUpdateDistanceMeters.toFloat())
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
            balancedSettings = LocationPerformanceManager.LocationPerformanceSettingsBalanced(
                config = LocationPerformanceSettingsType.Balanced.fromKey(appPrefs.locationSettingsBalancedPerformanceType),
                useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy
            ),
            batterySaverSettings = LocationPerformanceManager.LocationPerformanceSettingsBatterySaver(
                config = LocationPerformanceSettingsType.BatterySaver.fromKey(appPrefs.locationSettingsBatterySaverPerformanceType)
            )
        )
    }
}