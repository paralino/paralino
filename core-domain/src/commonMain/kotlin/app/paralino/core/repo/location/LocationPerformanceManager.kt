package app.paralino.core.repo.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationPerformanceRequest
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.utils.extensions.isOlderThan
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
import kotlin.time.Instant

class LocationPerformanceManager constructor(
    private val scope: ICoroutineScopeProvider,
    private val deviceStatus: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) {

    private val _locationPerformanceType = MutableStateFlow(
        LocationPerformanceModel(
            isBoostedManually = appPrefs.trackingBoostEndTime?.let { it > nowUTC() } == true,
            balancedPerformanceSettings = LocationPerformanceSettingsBalanced(
                config = LocationPerformanceSettingsType.Balanced.fromKey(
                    appPrefs.locationSettingsBalancedPerformanceType
                ),
                useHighPerformanceLocation = appPrefs.locationSettingsBalancedHighAccuracy
            ),
            batterySaverPerformanceSettings = LocationPerformanceSettingsBatterySaver(
                config = LocationPerformanceSettingsType.BatterySaver.fromKey(
                    appPrefs.locationSettingsBatterySaverPerformanceType
                )
            ),
            showLocationIndicator = appPrefs.locationSettingsShowIndicatorIOS,
            pausesLocationAutomatically = appPrefs.locationSettingsAutoPauseIOS
        )
    )
    val locationPerformanceType: StateFlow<LocationPerformanceModel> = _locationPerformanceType
    private var performanceUpdateJob: Job? = null
    private var locationBoostTracker = mutableMapOf<Long, LocationPerformanceType>()
    private var performanceResetScheduleTime: Pair<Instant, Duration>? = null

    private val veryHighBoostWindow = 10.minutes
    private val highBoostWindow = 20.minutes

    private val highPerformanceSettings: LocationPerformanceSettingsType.HighPerformance
        get() = LocationPerformanceSettingsType
            .HighPerformance
            .fromKey(appPrefs.locationSettingsHighPerformanceDurationType ?: appPrefs.locationSettingsHighPerformanceDurationTypeFallback)

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
                duration = performanceType.defaultDuration
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
        schedulePerformanceReset(type = LocationPerformanceType.BALANCED, resetDelay = 30.seconds)
    }

    fun updateLocationSettings(
        action: (LocationPerformanceModel) -> LocationPerformanceModel
    ) {
        _locationPerformanceType.update(action)
    }

    fun updateLocationSettings(
        useHighPerformanceLocation: Boolean,
        balancedPerformanceSettings: LocationPerformanceSettingsType.Balanced,
        batterySaverPerformanceSettings: LocationPerformanceSettingsType.BatterySaver
    ) {
        _locationPerformanceType.update {
            it.copy(
                balancedPerformanceSettings = LocationPerformanceSettingsBalanced(
                    config = balancedPerformanceSettings,
                    useHighPerformanceLocation = useHighPerformanceLocation
                ),
                batterySaverPerformanceSettings = LocationPerformanceSettingsBatterySaver(
                    config = batterySaverPerformanceSettings
                )
            )
        }
    }

    fun resetTimestamps() {
        locationBoostTracker = mutableMapOf()
    }

    fun checkPerformanceResetState() {
        // fallback check in case reset gets canceled for whatever reason
        // here we give it enough buffer and reset
        performanceResetScheduleTime?.let {
            val currentType = locationPerformanceType.value.current
            if (it.first.plus(it.second).isOlderThan(10.seconds)) {
                logg.tag("location_performance")
                    .w("Device performance reset is overdue, schedule reset again")
                schedulePerformanceReset(
                    type = LocationPerformanceType.BALANCED,
                    resetDelay = 10.seconds
                )
            } else if (currentType > getCorrectType(type = currentType, logInfo = false)) {
                performanceUpdateJob?.cancel()
                setPerformance(type = LocationPerformanceType.BALANCED, allowDowngrade = true)
                performanceResetScheduleTime = null
            }
        }
    }

    fun isPerformanceResetScheduledIn(threshold: Duration): Boolean {
        val schedule = performanceResetScheduleTime ?: return false
        val remaining = schedule.first.plus(schedule.second) - nowUTC()
        return remaining.isPositive() && remaining <= threshold
    }

    private fun preparePerformanceReset(
        request: LocationPerformanceRequest
    ) {
        // if locations is not yet resolved and someone requests location multiple times, cancel or increase reset
        // that way device will get more time to fetch high quality location

        if (request.type == LocationPerformanceType.MANUAL_BOOST || _locationPerformanceType.value.current == LocationPerformanceType.MANUAL_BOOST) {
            logg.tag("location_performance")
                .v("Device in manual boost, ignore performance reset for (${request.type})")

            val remainingDuration = appPrefs.trackingBoostEndTime?.let { it - nowUTC() }?.takeIf { it.isPositive() }
            if (remainingDuration != null) {
                schedulePerformanceReset(
                    type = LocationPerformanceType.BALANCED,
                    resetDelay = remainingDuration
                )
            } else {
                schedulePerformanceReset(
                    type = LocationPerformanceType.BALANCED,
                    resetDelay = 10.seconds
                )
            }
        } else if (request.type == LocationPerformanceType.VERY_HIGH) {
            schedulePerformanceReset(type = request.type.stepDown, resetDelay = 60.seconds)
        } else if (_locationPerformanceType.value.current < LocationPerformanceType.VERY_HIGH) {
            schedulePerformanceReset(
                type = request.type.stepDown,
                resetDelay = request.duration
            )
        } else {
            logg.tag("location_performance")
                .v("Skip location performance reset (${request.type}) from ${_locationPerformanceType.value.current}")
        }
    }

    private fun schedulePerformanceReset(
        type: LocationPerformanceType,
        resetDelay: Duration
    ) {
        performanceResetScheduleTime = nowUTC() to resetDelay
        performanceUpdateJob?.cancel()
        performanceUpdateJob = scope.userSessionIOScope.launch {
            logg.tag("location_performance")
                .v("Schedule location performance reset from ${_locationPerformanceType.value.current} to $type in $resetDelay")
            delay(resetDelay)
            val isManualBoostExpired = appPrefs.trackingBoostEndTime?.let { it < nowUTC() } ?: true
            if (_locationPerformanceType.value.current < LocationPerformanceType.MANUAL_BOOST) {
                logg.tag("location_performance")
                    .d("Reset location performance from ${_locationPerformanceType.value.current} to $type")
                setPerformance(type = type, allowDowngrade = true)
                performanceResetScheduleTime = null

                if (type.stepDown != type) {
                    schedulePerformanceReset(
                        type = type.stepDown,
                        resetDelay = type.defaultDuration
                    )
                }
            } else if (isManualBoostExpired) {
                disableManualBoost()
                performanceResetScheduleTime = null
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
        val last2Hours = nowUTC().minus(2, DateTimeUnit.HOUR).epochSeconds

        // keep only last few hours as record
        locationBoostTracker = locationBoostTracker.filter { it.key > last2Hours }.toMutableMap()

        val newType = getCorrectType(type = type, logInfo = true)

        locationBoostTracker[now] = newType
        _locationPerformanceType.value = _locationPerformanceType.value.copy(
            old = _locationPerformanceType.value.current,
            requested = type,
            current = newType
        )
    }

    fun getPerformanceAllowances(): List<LocationPerformanceAllowance> {
        val durations = calculateBoostDurations()
        val settings = highPerformanceSettings
        return listOf(
            LocationPerformanceAllowance(
                type = LocationPerformanceType.HIGH,
                window = highBoostWindow,
                allowed = settings.allowedHighPerformanceDuration,
                used = durations.high
            ),
            LocationPerformanceAllowance(
                type = LocationPerformanceType.VERY_HIGH,
                window = veryHighBoostWindow,
                allowed = settings.allowedVeryHighPerformanceDuration,
                used = durations.veryHigh
            )
        )
    }

    private fun getCorrectType(
        type: LocationPerformanceType,
        logInfo: Boolean
    ): LocationPerformanceType {
        val durations = calculateBoostDurations()

        val newType = adjustPerformanceType(
            type = type,
            currentMediumBoostDuration = durations.medium,
            currentHighBoostDuration = durations.high,
            currentVeryHighBoostDuration = durations.veryHigh,
            batteryLevel = deviceStatus.batteryLevel,
            lowPowerMode = deviceStatus.isLowPowerMode
        )

        if (logInfo) {
            logg.tag("location_performance")
                .v(
                    "Performance - requested: $type, new: $newType, old: ${_locationPerformanceType.value.current} [${_locationPerformanceType.value}] - for: " +
                            "BAT: ${deviceStatus.batteryLevel}%,\n" +
                            "time: ${nowUTC().epochSeconds},\n" +
                            "VERY_HIGH: ${durations.veryHigh},\n" +
                            "HIGH: ${durations.high},\n" +
                            "MEDIUM: ${durations.medium}"
                )
        }

        return newType
    }

    private fun calculateBoostDurations(): BoostDurations {
        val now = nowUTC().epochSeconds
        val last10min = nowUTC().minus(veryHighBoostWindow).epochSeconds
        val last20min = nowUTC().minus(highBoostWindow).epochSeconds

        // take only recent period for optimization
        val iterator10Min = locationBoostTracker.filter { it.key > last10min }.toList()
        val iterator20Min = locationBoostTracker.filter { it.key > last20min }.toList()

        var currentVeryHighBoostDuration = Duration.ZERO
        var currentHighBoostDuration = Duration.ZERO
        var currentMediumBoostDuration = Duration.ZERO

        // sum duration of VERY_HIGH requests in 10 minutes
        iterator10Min.forEachIndexed { index, pair ->
            // don't consider user own boost
            if (pair.second < LocationPerformanceType.MANUAL_BOOST) {
                val next = iterator10Min.getOrNull(index + 1)
                val durationInSec = ((next?.first ?: now) - pair.first).seconds

                // consider very_high performance in the last 10 min
                if (pair.second >= LocationPerformanceType.VERY_HIGH) {
                    currentVeryHighBoostDuration += durationInSec
                }
            }
        }

        // sum duration of HIGH and MEDIUM_HIGH requests in last 20 min
        iterator20Min.forEachIndexed { index, pair ->
            // don't consider user own boost or location request
            if (pair.second < LocationPerformanceType.VERY_HIGH) {
                val next = iterator20Min.getOrNull(index + 1)
                val durationInSec = ((next?.first ?: now) - pair.first).seconds

                if (pair.second >= LocationPerformanceType.HIGH) {
                    currentHighBoostDuration += durationInSec
                }
                if (pair.second >= LocationPerformanceType.MEDIUM_HIGH) {
                    currentMediumBoostDuration += durationInSec
                }
            }
        }

        return BoostDurations(
            veryHigh = currentVeryHighBoostDuration,
            high = currentHighBoostDuration,
            medium = currentMediumBoostDuration
        )
    }

    private fun adjustPerformanceType(
        type: LocationPerformanceType,
        currentMediumBoostDuration: Duration,
        currentHighBoostDuration: Duration,
        currentVeryHighBoostDuration: Duration,
        batteryLevel: Int?,
        lowPowerMode: Boolean
    ): LocationPerformanceType {

        // allow only certain amount of boost per hour
        val highPerformanceSettings = this.highPerformanceSettings
        val balancedPerformanceSettings = LocationPerformanceSettingsType
            .Balanced
            .fromKey(appPrefs.locationSettingsBalancedPerformanceType)

        val didConsumedMediumPerformanceAllowance = currentMediumBoostDuration >= highPerformanceSettings.allowedMediumPerformanceDuration
        val didConsumedHighPerformanceAllowance = currentHighBoostDuration >= highPerformanceSettings.allowedHighPerformanceDuration
        val didConsumedVeryHighPerformanceAllowance = currentVeryHighBoostDuration >= highPerformanceSettings.allowedVeryHighPerformanceDuration

        // take certain range of battery in account
        // assume decent battery level if battery level unavailable
        val lowBatteryLevelThreshold = when (balancedPerformanceSettings) {
            LocationPerformanceSettingsType.Balanced.Level1,
            LocationPerformanceSettingsType.Balanced.Level2,
            LocationPerformanceSettingsType.Balanced.Level3 -> 30
            LocationPerformanceSettingsType.Balanced.Level4 -> 20
            LocationPerformanceSettingsType.Balanced.Level5 -> 15
        }
        val batteryLevelAdjusted = batteryLevel ?: lowBatteryLevelThreshold
        val batteryVeryLow = batteryLevelAdjusted < 10
        val batteryLow = batteryLevelAdjusted < lowBatteryLevelThreshold
        val shouldUseLowPowerMode = batteryLow || (lowPowerMode && appPrefs.locationBatterySaverRespectsDeviceLowPower)

        return when (type) {
            LocationPerformanceType.MANUAL_BOOST -> {
                // always allow manual boost
                LocationPerformanceType.MANUAL_BOOST
            }

            LocationPerformanceType.VERY_HIGH -> {
                if (didConsumedVeryHighPerformanceAllowance) {
                    // user consumed very high performance allowance, check with HIGH
                    adjustPerformanceType(
                        type = LocationPerformanceType.HIGHER_FOCUS,
                        currentMediumBoostDuration = currentMediumBoostDuration,
                        currentHighBoostDuration = currentHighBoostDuration,
                        currentVeryHighBoostDuration = currentVeryHighBoostDuration,
                        batteryLevel = batteryLevel,
                        lowPowerMode = lowPowerMode
                    )
                } else {
                    LocationPerformanceType.VERY_HIGH
                }
            }

            LocationPerformanceType.HIGH,
            LocationPerformanceType.HIGHER_FOCUS -> {
                if (batteryVeryLow) {
                    LocationPerformanceType.BATTERY_SAVER
                } else if (shouldUseLowPowerMode) {
                    if (didConsumedMediumPerformanceAllowance || didConsumedHighPerformanceAllowance) {
                        // when battery is less than lowBatteryLevelThreshold, and we spent all medium and high allowance
                        // revert to BATTERY_SAVER to conserve battery
                        LocationPerformanceType.BATTERY_SAVER
                    } else {
                        // otherwise go for medium performance to preserve more battery
                        LocationPerformanceType.MEDIUM_HIGH
                    }
                } else if (didConsumedHighPerformanceAllowance) {
                    if (didConsumedMediumPerformanceAllowance) {
                        // user consumed both high and medium performance allowance
                        LocationPerformanceType.BALANCED
                    } else {
                        // user consumed only high performance allowance but there is still some time left in medium
                        LocationPerformanceType.MEDIUM_HIGH
                    }
                } else {
                    type
                }
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                if (batteryVeryLow) LocationPerformanceType.BATTERY_SAVER
                else if (shouldUseLowPowerMode) LocationPerformanceType.BALANCED
                else if (didConsumedMediumPerformanceAllowance) LocationPerformanceType.BALANCED
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
        val balancedPerformanceSettings: LocationPerformanceSettingsBalanced,
        val batterySaverPerformanceSettings: LocationPerformanceSettingsBatterySaver
    ) {

        constructor(
            isBoostedManually: Boolean,
            balancedPerformanceSettings: LocationPerformanceSettingsBalanced,
            batterySaverPerformanceSettings: LocationPerformanceSettingsBatterySaver,
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

        val isThrottlingVeryHighRequest get() = if (requested >= LocationPerformanceType.VERY_HIGH) {
            // checking < High since High and VeryHigh have the same config
            // VeryHigh just allows for additional time in high when requested
            current < LocationPerformanceType.HIGH
        } else {
            false
        }

        val isThrottlingPerformance get() = if (requested >= LocationPerformanceType.HIGH) {
            current < LocationPerformanceType.HIGH
        } else {
            current < requested
        }

        val isSlow get() = isThrottlingPerformance || current <= LocationPerformanceType.BALANCED
        val isBatterySaverOn get() = current <= LocationPerformanceType.BATTERY_SAVER
    }

    data class LocationPerformanceSettingsBalanced(
        val config: LocationPerformanceSettingsType.Balanced,
        val useHighPerformanceLocation: Boolean
    )

    data class LocationPerformanceSettingsBatterySaver(
        val config: LocationPerformanceSettingsType.BatterySaver
    )

    data class LocationPerformanceAllowance(
        val type: LocationPerformanceType,
        val window: Duration,
        val allowed: Duration,
        val used: Duration
    ) {
        val remaining: Duration get() = (allowed - used).coerceAtLeast(Duration.ZERO)
    }

    private data class BoostDurations(
        val veryHigh: Duration,
        val high: Duration,
        val medium: Duration
    )
}