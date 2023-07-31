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
                delay(10_000)
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

    fun locationBoostRequested(groupId: String) {
        logg.tag("location_performance").v("Location boost requested for $groupId")
        setPerformance(LocationPerformanceType.HIGH)
        schedulePerformanceReset()
    }

    private fun schedulePerformanceReset(resetDelay: Long = 12_000) {
        performanceUpdateJob?.cancel()
        performanceUpdateJob = scope.userSessionIOScope.launch {
            delay(resetDelay)
            logg.tag("location_performance").v("Reset location performance")
            setPerformance(LocationPerformanceType.BALANCED)
        }
    }

    /**
     * will adjust performance flag based on battery and current time spent in performance mode
     * TODO: when user battery settings are added, take them in account here
     */
    private fun setPerformance(type: LocationPerformanceType) {

        val now = nowUTC().epochSeconds
        val lastHour = nowUTC().minus(1, DateTimeUnit.HOUR).epochSeconds
        val last4Hours = nowUTC().minus(4, DateTimeUnit.HOUR).epochSeconds

        // keep only last 4 hours as record
        locationBoostTracker = locationBoostTracker.filter { it.key > last4Hours }.toMutableMap()

        // take only last hour for optimization
        val iterator = locationBoostTracker
            .filter { it.key > lastHour }
            .toList()

        var lastHighBoostDiff = Duration.ZERO
        var lastMediumBoostDiff = Duration.ZERO

        // sum duration of HIGH and MEDIUM_HIGH requests in last hour
        iterator.forEachIndexed { index, pair ->
            val next = iterator.getOrNull(index + 1)
            val durationInSec = ((next?.first ?: now) - pair.first).seconds
            if (pair.second == LocationPerformanceType.HIGH) {
                // count HIGH performance time duration as MEDIUM_HIGH as well
                lastHighBoostDiff += durationInSec
                lastMediumBoostDiff += durationInSec
            } else if (pair.second == LocationPerformanceType.MEDIUM_HIGH) {
                lastMediumBoostDiff += durationInSec
            }
        }

        // allow only certain amount of medium or higher boost (HIGH and MEDIUM_HIGH) per hour
        val timeCriteriaLow = lastMediumBoostDiff > 40.minutes

        // allow only certain amount of high boost per hour
        val timeCriteriaHigh = lastHighBoostDiff > 20.minutes

        // take certain range of battery in account
        val batteryLow = deviceStatus.batteryLevel < 10
        val batteryMedium = deviceStatus.batteryLevel < 20

        val newType = when (type) {
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
                } else type
            }
            LocationPerformanceType.MEDIUM_HIGH -> {
                if (batteryLow) LocationPerformanceType.BATTERY_SAVER
                else if (timeCriteriaLow) LocationPerformanceType.BALANCED
                else type
            }
            LocationPerformanceType.BALANCED -> {
                if (batteryLow) LocationPerformanceType.BATTERY_SAVER
                else type
            }
            LocationPerformanceType.BATTERY_SAVER -> {
                // accept
                type
            }
        }

        logg.tag("location_performance")
            .v(
                "Performance - requested: $type, new: $newType, old: ${_locationPerformanceType.value} - for: " +
                        "BAT: ${deviceStatus.batteryLevel}%, " +
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

    data class LocationPerformanceModel(
        val old: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val requested: LocationPerformanceType = LocationPerformanceType.BALANCED,
        val current: LocationPerformanceType = LocationPerformanceType.BALANCED,
    ) {
        val isThrottling get() = requested > current
    }
}