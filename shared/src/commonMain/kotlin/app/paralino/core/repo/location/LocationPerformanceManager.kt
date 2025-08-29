package app.paralino.core.repo.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocationPerformanceManager constructor(
    private val scope: ICoroutineScopeProvider,
    private val deviceStatus: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) {

    private val _locationPerformanceType = MutableStateFlow(
        LocationPerformanceModel(
            isBoostedManually = appPrefs.trackingBoostEndTime?.let { it > nowUTC() } == true,
            balancedPerformanceSettings = LocationPerformanceSettingsType.Balanced.fromKey(
                appPrefs.locationSettingsBalancedPerformanceType
            ).let {
                LocationPerformanceSettings(
                    settingsType = it,
                    useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy
                )
            },
            batterySaverPerformanceSettings = LocationPerformanceSettingsType.BatterySaver.fromKey(
                appPrefs.locationSettingsBatterySaverPerformanceType
            ).let {
                LocationPerformanceSettings(
                    settingsType = it,
                    useHighPerformanceLocation = false
                )
            },
            showLocationIndicator = appPrefs.locationSettingsShowIndicatorIOS,
            pausesLocationAutomatically = appPrefs.locationSettingsAutoPauseIOS
        )
    )
    val locationPerformanceType: StateFlow<LocationPerformanceModel> = _locationPerformanceType
    private var performanceUpdateJob: Job? = null
    private var locationBoostTracker = mutableMapOf<Long, LocationPerformanceType>()

    /**
     * Used for subscription in iOS
     */
    fun locationPerformanceStatus(onChange: (LocationPerformanceModel) -> Unit) {
        locationPerformanceType.onEach { onChange(it) }.launchIn(scope.defaultIOScope)
    }

    fun locationBoostRequested(
        performanceType: LocationPerformanceType,
        groupId: GroupOrSpaceId?,
        from: String?,
    ) {
        locationBoostRequested(
            request = LocationPerformanceRequest(
                type = performanceType,
                durationMillis = performanceType.defaultDurationMillis
            ),
            groupId = groupId,
            from = from
        )
    }

    fun locationBoostRequested(
        request: LocationPerformanceRequest,
        groupId: GroupOrSpaceId?,
        from: String?,
    ) {
        logg.tag("location_performance")
            .v("Location boost (${request.type}) requested for $groupId from $from")
        setPerformance(type = request.type, allowDowngrade = false)
        preparePerformanceReset(request = request)
    }

    fun disableManualBoost() {
        setPerformance(type = LocationPerformanceType.HIGH, allowDowngrade = true)
        schedulePerformanceReset(type = LocationPerformanceType.BALANCED, resetDelayMillis = 30_000)
    }

    fun updateLocationSettings(
        action: (LocationPerformanceModel) -> LocationPerformanceModel
    ) {
        _locationPerformanceType.update(action)
    }

    fun updateLocationSettings(
        useHighPerformanceLocation: Boolean,
        balancedPerformanceSettings: LocationPerformanceSettingsType,
        batterySaverPerformanceSettings: LocationPerformanceSettingsType
    ) {
        _locationPerformanceType.update {
            it.copy(
                balancedPerformanceSettings = LocationPerformanceSettings(
                    settingsType = balancedPerformanceSettings,
                    useHighPerformanceLocation = useHighPerformanceLocation
                ),
                batterySaverPerformanceSettings = LocationPerformanceSettings(
                    settingsType = batterySaverPerformanceSettings,
                    useHighPerformanceLocation = false
                )
            )
        }
    }

    private fun preparePerformanceReset(
        request: LocationPerformanceRequest
    ) {
        // if locations is not yet resolved and someone requests location multiple times, cancel or increase reset
        // that way device will get more time to fetch high quality location

        if (request.type == LocationPerformanceType.MANUAL_BOOST || _locationPerformanceType.value.current == LocationPerformanceType.MANUAL_BOOST) {
            logg.tag("location_performance")
                .v("Device in manual boost, ignore performance reset for (${request.type})")
            performanceUpdateJob?.cancel()
            val remainingDuration =
                appPrefs.trackingBoostEndTime?.let { it - nowUTC() }?.takeIf { it.isPositive() }
            if (remainingDuration != null) {
                schedulePerformanceReset(
                    type = LocationPerformanceType.BALANCED,
                    resetDelayMillis = remainingDuration.inWholeMilliseconds
                )
            } else {
                schedulePerformanceReset(
                    type = LocationPerformanceType.BALANCED,
                    resetDelayMillis = 10.seconds.inWholeMilliseconds
                )
            }
        } else if (request.type == LocationPerformanceType.VERY_HIGH) {
            schedulePerformanceReset(type = request.type.stepDown, resetDelayMillis = 60_000)
        } else if (_locationPerformanceType.value.current < LocationPerformanceType.VERY_HIGH) {
            schedulePerformanceReset(
                type = request.type.stepDown,
                resetDelayMillis = request.durationMillis
            )
        } else {
            logg.tag("location_performance")
                .v("Skip location performance reset (${request.type}) from ${_locationPerformanceType.value.current}")
        }
    }

    private fun schedulePerformanceReset(
        type: LocationPerformanceType,
        resetDelayMillis: Long
    ) {
        performanceUpdateJob?.cancel()
        performanceUpdateJob = scope.userSessionIOScope.launch {
            logg.tag("location_performance")
                .v("Schedule location performance reset from ${_locationPerformanceType.value.current} to $type in $resetDelayMillis")
            delay(resetDelayMillis)
            val isManualBoostExpired = appPrefs.trackingBoostEndTime?.let { it < nowUTC() } ?: true
            if (_locationPerformanceType.value.current < LocationPerformanceType.MANUAL_BOOST) {
                logg.tag("location_performance")
                    .v("Reset location performance from ${_locationPerformanceType.value.current} to $type")
                setPerformance(type = type, allowDowngrade = true)
                if (type.stepDown != type) {
                    schedulePerformanceReset(
                        type = type.stepDown,
                        resetDelayMillis = type.defaultDurationMillis
                    )
                }
            } else if (isManualBoostExpired) {
                disableManualBoost()
            }
        }
    }

    /**
     * will adjust performance flag based on battery and current time spent in performance mode
     */
    private fun setPerformance(type: LocationPerformanceType, allowDowngrade: Boolean = true) {

        if (!allowDowngrade && type < _locationPerformanceType.value.current) {
            logg.tag("location_performance")
                .v("Skip performance update to $type, allowDowngrade ($allowDowngrade)")
            return
        }

        val now = nowUTC().epochSeconds
        val last20min = nowUTC().minus(20, DateTimeUnit.MINUTE).epochSeconds
        val lastHour = nowUTC().minus(1, DateTimeUnit.HOUR).epochSeconds
        val last4Hours = nowUTC().minus(4, DateTimeUnit.HOUR).epochSeconds

        // keep only last 4 hours as record
        locationBoostTracker = locationBoostTracker.filter { it.key > last4Hours }.toMutableMap()

        // take only last hour for optimization
        val hourIterator = locationBoostTracker.filter { it.key > lastHour }.toList()
        val minuteIterator = locationBoostTracker.filter { it.key > last20min }.toList()

        var lastVeryHighBoostDiff = Duration.ZERO
        var lastHighBoostDiff = Duration.ZERO
        var lastMediumBoostDiff = Duration.ZERO

        // sum duration of VERY_HIGH requests in 20 minutes
        minuteIterator.forEachIndexed { index, pair ->
            // don't consider user own boost
            if (pair.second < LocationPerformanceType.MANUAL_BOOST) {
                val next = minuteIterator.getOrNull(index + 1)
                val durationInSec = ((next?.first ?: now) - pair.first).seconds

                // consider very_high performance in the last 20 min
                if (pair.second >= LocationPerformanceType.VERY_HIGH) {
                    lastVeryHighBoostDiff += durationInSec
                }
            }
        }

        // sum duration of HIGH and MEDIUM_HIGH requests in last hour
        hourIterator.forEachIndexed { index, pair ->
            // don't consider user own boost
            if (pair.second < LocationPerformanceType.MANUAL_BOOST) {
                val next = hourIterator.getOrNull(index + 1)
                val durationInSec = ((next?.first ?: now) - pair.first).seconds

                if (pair.second >= LocationPerformanceType.HIGH) {
                    lastHighBoostDiff += durationInSec
                }
                if (pair.second >= LocationPerformanceType.MEDIUM_HIGH) {
                    lastMediumBoostDiff += durationInSec
                }
            }
        }

        val newType = adjustPerformanceType(
            type = type,
            lastMediumBoostDiff = lastMediumBoostDiff,
            lastHighBoostDiff = lastHighBoostDiff,
            lastVeryHighBoostDiff = lastVeryHighBoostDiff,
            batteryLevel = deviceStatus.batteryLevel,
            lowPowerMode = deviceStatus.isLowPowerMode
        )

        logg.tag("location_performance")
            .v(
                "Performance - requested: $type, new: $newType, old: ${_locationPerformanceType.value.current} [${_locationPerformanceType.value}] - for: " +
                        "BAT: ${deviceStatus.batteryLevel}%, " +
                        "VERY_HIGH: $lastVeryHighBoostDiff, " +
                        "HIGH: $lastHighBoostDiff, " +
                        "MEDIUM: $lastMediumBoostDiff"
            )

        locationBoostTracker.put(now, newType)
        _locationPerformanceType.value = _locationPerformanceType.value.copy(
            old = _locationPerformanceType.value.current,
            requested = type,
            current = newType
        )
    }

    private fun adjustPerformanceType(
        type: LocationPerformanceType,
        lastMediumBoostDiff: Duration,
        lastHighBoostDiff: Duration,
        lastVeryHighBoostDiff: Duration,
        batteryLevel: Int?,
        lowPowerMode: Boolean
    ): LocationPerformanceType {

        // allow only certain amount of boost per hour
        val highPerformanceDuration = LocationPerformanceSettingsType
            .HighPerformance
            .fromKey(appPrefs.locationSettingsHighPerformanceDurationType)
            .maxAllowedDuration
            .coerceIn(10.minutes, 60.minutes)
        val timeCriteriaHigh = lastHighBoostDiff > highPerformanceDuration
        val timeCriteriaLow = lastMediumBoostDiff > highPerformanceDuration.plus(20.minutes)
            .coerceIn(10.minutes, 60.minutes)
        val timeCriteriaVeryHigh = lastVeryHighBoostDiff > 3.minutes

        // take certain range of battery in account
        // assume decent battery level if battery level unavailable
        val batteryLevelAdjusted = batteryLevel ?: 30
        val batteryVeryLow = batteryLevelAdjusted < 10
        val batteryLow = batteryLevelAdjusted < 30
        val shouldUseLowPowerMode = batteryLow || (lowPowerMode && appPrefs.locationBatterySaverRespectsDeviceLowPower)

        return when (type) {
            LocationPerformanceType.MANUAL_BOOST -> {
                // always allow manual boost
                LocationPerformanceType.MANUAL_BOOST
            }

            LocationPerformanceType.VERY_HIGH -> {
                if (timeCriteriaVeryHigh) {
                    // user consumed very high performance allowance, check with HIGH
                    adjustPerformanceType(
                        type = LocationPerformanceType.HIGH,
                        lastMediumBoostDiff = lastMediumBoostDiff,
                        lastHighBoostDiff = lastHighBoostDiff,
                        lastVeryHighBoostDiff = lastVeryHighBoostDiff,
                        batteryLevel = batteryLevel,
                        lowPowerMode = lowPowerMode
                    )
                } else {
                    LocationPerformanceType.VERY_HIGH
                }
            }

            LocationPerformanceType.HIGH -> {
                if (batteryVeryLow) {
                    LocationPerformanceType.BATTERY_SAVER
                } else if (shouldUseLowPowerMode) {
                    if (timeCriteriaLow || timeCriteriaHigh) {
                        // when battery is less than 30% and we spent all medium and high allowance
                        // revert to BALANCED to conserve battery
                        LocationPerformanceType.BATTERY_SAVER
                    } else {
                        // otherwise go for medium performance to preserve more battery
                        LocationPerformanceType.MEDIUM_HIGH
                    }
                } else if (timeCriteriaLow) {
                    // user consumed both high and medium performance allowance
                    LocationPerformanceType.BALANCED
                } else if (timeCriteriaHigh) {
                    // user consumed only high performance allowance but there is still some time left in medium
                    LocationPerformanceType.MEDIUM_HIGH
                } else {
                    LocationPerformanceType.HIGH
                }
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                if (batteryVeryLow) LocationPerformanceType.BATTERY_SAVER
                else if (shouldUseLowPowerMode) LocationPerformanceType.BALANCED
                else if (timeCriteriaLow) LocationPerformanceType.BALANCED
                else LocationPerformanceType.MEDIUM_HIGH
            }

            LocationPerformanceType.BALANCED -> {
                if (shouldUseLowPowerMode) LocationPerformanceType.BATTERY_SAVER
                else LocationPerformanceType.BALANCED
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                if (batteryVeryLow) LocationPerformanceType.HIGH_BATTERY_SAVER
                else LocationPerformanceType.BATTERY_SAVER
            }

            LocationPerformanceType.HIGH_BATTERY_SAVER -> {
                // accept
                LocationPerformanceType.HIGH_BATTERY_SAVER
            }
        }
    }

    data class LocationPerformanceModel(
        val old: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val requested: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val current: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val showLocationIndicator: Boolean = false,
        val pausesLocationAutomatically: Boolean = true,
        val balancedPerformanceSettings: LocationPerformanceSettings,
        val batterySaverPerformanceSettings: LocationPerformanceSettings
    ) {

        constructor(
            isBoostedManually: Boolean,
            balancedPerformanceSettings: LocationPerformanceSettings,
            batterySaverPerformanceSettings: LocationPerformanceSettings,
            showLocationIndicator: Boolean,
            pausesLocationAutomatically: Boolean,
        ) : this(
            old = LocationPerformanceType.BALANCED,
            requested = if (isBoostedManually) LocationPerformanceType.MANUAL_BOOST else LocationPerformanceType.BALANCED,
            current = if (isBoostedManually) LocationPerformanceType.MANUAL_BOOST else LocationPerformanceType.BALANCED,
            showLocationIndicator = showLocationIndicator,
            pausesLocationAutomatically = pausesLocationAutomatically,
            balancedPerformanceSettings = balancedPerformanceSettings,
            batterySaverPerformanceSettings = batterySaverPerformanceSettings
        )

        val isThrottling get() = requested > current
        val isThrottlingLocationRequest get() = isThrottling && requested == LocationPerformanceType.VERY_HIGH
        val isSlow get() = isThrottling || current <= LocationPerformanceType.BALANCED
    }

    data class LocationPerformanceSettings(
        val updateIntervalMillis: Long,
        val minUpdateIntervalMillis: Long,
        val minUpdateDistanceMeters: Int,
        val useHighPerformanceLocation: Boolean
    ) {
        constructor(
            settingsType: LocationPerformanceSettingsType,
            useHighPerformanceLocation: Boolean
        ) : this(
            updateIntervalMillis = settingsType.updateInterval.inWholeMilliseconds,
            minUpdateIntervalMillis = settingsType.minUpdateInterval.inWholeMilliseconds,
            minUpdateDistanceMeters = settingsType.minUpdateDistanceMeters,
            useHighPerformanceLocation = useHighPerformanceLocation
        )
    }
}