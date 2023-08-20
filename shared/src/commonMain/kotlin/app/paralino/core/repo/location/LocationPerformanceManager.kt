package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocationPerformanceManager constructor(
    private val documentApi: IDocumentsApi,
    val scope: ICoroutineScopeProvider,
    val deviceStatus: IDeviceApiProvider,
) : BaseRepository() {

    private val _locationPerformanceType = MutableStateFlow(LocationPerformanceModel())
    val locationPerformanceType: StateFlow<LocationPerformanceModel> = _locationPerformanceType
    private var performanceUpdateJob: Job? = null
    private var locationBoostTracker = mutableMapOf<Long, LocationPerformanceType>()

    fun boostOnMapFlow(groupId: String): Flow<String> {
        return flow {
            do {
                logg.tag("location_performance").v("User is looking at map")
                emit("User is looking at map")
                boostOnMap(groupId)
                delay(20_000)
            } while (currentCoroutineContext().isActive)
        }.catch { emit("Error while boosting location") }
    }

    suspend fun boostOnMap(groupId: String) {

        // TODO most likely this is not needed anymore
//        if (locationSharingManager.isSharingActiveForGroup(groupId)) {
//            setPerformance(LocationPerformanceType.HIGH)
//            schedulePerformanceReset()
//        }

        triggerRemoteOnly {
            try {
                documentApi.updateDocument(
                    databaseType = BackendDatabaseType.DATA,
                    collectionId = BackendResourceIDs.collectionLocationFocusID,
                    docId = "$groupId${config.userId}".take(BackendResourceIDs.databaseIdMaxLength),
                    data = mapOf(
                        "groupId" to groupId
                    )
                )
                Unit
            } catch (e: Exception) {
                if (e.asRemoteException() is NotFoundException) {
                    // TODO review if we should create document
                }
                throw e
            }
        }
    }

    fun locationBoostRequested(
        groupId: String,
        performanceType: LocationPerformanceType = LocationPerformanceType.HIGH
    ) {
        logg.tag("location_performance").v("Location boost ($performanceType) requested for $groupId")
        setPerformance(performanceType, allowDowngrade = false)

        // TODO check if someone is requesting location multiple times and don't reset performance in that case
        // if locations is not yet resolved and someone requests location multiple times, cancel or increase reset
        // that way device will get more time to fetch high quality location

        if (performanceType == LocationPerformanceType.VERY_HIGH) {
            schedulePerformanceReset(LocationPerformanceType.HIGH, 60_000)
        } else if (_locationPerformanceType.value.current < LocationPerformanceType.VERY_HIGH) {
            if (performanceType < _locationPerformanceType.value.current) {
                schedulePerformanceReset(LocationPerformanceType.BALANCED, 45_000)
            } else {
                schedulePerformanceReset(LocationPerformanceType.BALANCED, 30_000)
            }
        } else {
            logg.tag("location_performance").v("Skip location performance reset ($performanceType)")
        }
    }

    private fun schedulePerformanceReset(
        type: LocationPerformanceType,
        resetDelayMillis: Long
    ) {
        performanceUpdateJob?.cancel()
        performanceUpdateJob = scope.userSessionIOScope.launch {
            logg.tag("location_performance").v("Schedule location performance reset from ${_locationPerformanceType.value.current} in $resetDelayMillis")
            delay(resetDelayMillis)
            logg.tag("location_performance").v("Reset location performance to $type")
            setPerformance(type = type, allowDowngrade = true)
        }
    }

    /**
     * will adjust performance flag based on battery and current time spent in performance mode
     * TODO: when user battery settings are added, take them in account here
     */
    private fun setPerformance(type: LocationPerformanceType, allowDowngrade: Boolean = true) {

        if (!allowDowngrade && type < _locationPerformanceType.value.current) {
            logg.tag("location_performance").v("Skip performance update to $type, allowDowngrade ($allowDowngrade)")
            return
        }

        logg.tag("location_performance").v("Adjust performance update ${_locationPerformanceType.value.current} -> $type")

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
            val next = minuteIterator.getOrNull(index + 1)
            val durationInSec = ((next?.first ?: now) - pair.first).seconds

            // consider very_high performance in the last 20 min
            if (pair.second >= LocationPerformanceType.VERY_HIGH) {
                lastVeryHighBoostDiff += durationInSec
            }
        }

        // sum duration of HIGH and MEDIUM_HIGH requests in last hour
        hourIterator.forEachIndexed { index, pair ->
            val next = hourIterator.getOrNull(index + 1)
            val durationInSec = ((next?.first ?: now) - pair.first).seconds

            if (pair.second >= LocationPerformanceType.HIGH) {
                lastHighBoostDiff += durationInSec
            }
            if (pair.second > LocationPerformanceType.MEDIUM_HIGH) {
                lastMediumBoostDiff += durationInSec
            }
        }

        val newType = adjustPerformanceType(
            type = type,
            lastMediumBoostDiff = lastMediumBoostDiff,
            lastHighBoostDiff = lastHighBoostDiff,
            lastVeryHighBoostDiff = lastVeryHighBoostDiff,
            batteryLevel = deviceStatus.batteryLevel
        )

        logg.tag("location_performance")
            .v(
                "Performance - requested: $type, new: $newType, old: ${_locationPerformanceType.value} - for: " +
                        "BAT: ${deviceStatus.batteryLevel}%, " +
                        "VERY_HIGH: $lastVeryHighBoostDiff, " +
                        "HIGH: $lastHighBoostDiff, " +
                        "MEDIUM: $lastMediumBoostDiff"
            )

        locationBoostTracker.put(now, newType)
        _locationPerformanceType.value = LocationPerformanceModel(
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
        batteryLevel: Int,
    ): LocationPerformanceType {

        // allow only certain amount of boost per hour
        val timeCriteriaLow = lastMediumBoostDiff > 40.minutes
        val timeCriteriaHigh = lastHighBoostDiff > 20.minutes
        val timeCriteriaVeryHigh = lastVeryHighBoostDiff > 3.minutes

        // take certain range of battery in account
        val batteryLow = batteryLevel < 10
        val batteryMedium = batteryLevel < 20

        return when (type) {
            LocationPerformanceType.VERY_HIGH -> {
                if (timeCriteriaVeryHigh) {
                    // user consumed very high performance allowance, check with HIGH
                    adjustPerformanceType(
                        type = LocationPerformanceType.HIGH,
                        lastMediumBoostDiff = lastMediumBoostDiff,
                        lastHighBoostDiff = lastHighBoostDiff,
                        lastVeryHighBoostDiff = lastVeryHighBoostDiff,
                        batteryLevel = batteryLevel
                    )
                } else {
                    LocationPerformanceType.VERY_HIGH
                }
            }

            LocationPerformanceType.HIGH -> {
                if (batteryLow) {
                    LocationPerformanceType.BATTERY_SAVER
                } else if (batteryMedium) {
                    if (timeCriteriaLow || timeCriteriaHigh) {
                        // when battery is less than 20% and we spent all medium and high allowance
                        // revert to BALANCED to conserve battery
                        LocationPerformanceType.BALANCED
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
                } else LocationPerformanceType.HIGH
            }

            LocationPerformanceType.MEDIUM_HIGH -> {
                if (batteryLow) LocationPerformanceType.BATTERY_SAVER
                else if (timeCriteriaLow) LocationPerformanceType.BALANCED
                else LocationPerformanceType.MEDIUM_HIGH
            }

            LocationPerformanceType.BALANCED -> {
                if (batteryLow) LocationPerformanceType.BATTERY_SAVER
                else LocationPerformanceType.BALANCED
            }

            LocationPerformanceType.BATTERY_SAVER -> {
                // accept
                LocationPerformanceType.BATTERY_SAVER
            }
        }
    }

    data class LocationPerformanceModel(
        val old: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val requested: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val current: LocationPerformanceType = LocationPerformanceType.BALANCED,
    ) {
        val isThrottling get() = requested > current
        val isThrottlingLocationRequest get() = isThrottling && requested == LocationPerformanceType.VERY_HIGH
        val isSlow get() = isThrottling || current <= LocationPerformanceType.BALANCED
    }
}