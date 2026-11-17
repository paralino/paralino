package app.paralino.core.domain.location.engine

import app.paralino.core.logg
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.provider.ConnectedWifi
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.location.LocationFilteringType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.WifiAnchorMemory
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LocationFilterProcessor(
    private val connectionStatus: IConnectionStatusProvider,
    private val wifiAnchorMemory: WifiAnchorMemory,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val scopeProvider: ICoroutineScopeProvider,
) {

    private val mutex = Mutex()
    private var wifi: ConnectedWifi? = null
    private var wifiTime: Instant? = null
    private var streak: Streak? = null
    private var processingJob: Job? = null

    fun start(
        currentLocationFlow: StateFlow<LocationUpdateDataFromDevice?>,
        pendingLocationFlow: MutableStateFlow<LocationUpdateDataFromDevice?>
    ) {
        processingJob?.cancel()
        processingJob = currentLocationFlow
            .filterNotNull()
            .onEach { location ->
                val filtered = filter(location) ?: return@onEach
                pendingLocationFlow.update { pending ->
                    mostRelevant(pending = pending, data = filtered)
                }
            }
            .launchIn(scopeProvider.userSessionIOScope)
    }

    fun stop() {
        processingJob?.cancel()
        processingJob = null
    }

    suspend fun filter(data: LocationUpdateDataFromDevice): LocationUpdateDataFromDevice? {
        return try {
            filterByWifi(
                data = data,
                filtering = locationPerformanceManager.locationPerformanceType.value.filtering
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logg.tag("location_service").w("Error filtering location: ${e.message}")
            data
        }
    }

    private fun mostRelevant(
        pending: LocationUpdateDataFromDevice?,
        data: LocationUpdateDataFromDevice
    ): LocationUpdateDataFromDevice? {
        val potentialUpdate = LocationUpdateDataFromDevice.getBetter(pending, data)
        if (pending == null || potentialUpdate == null) {
            return potentialUpdate
        }
        val performance = locationPerformanceManager.locationPerformanceType.value
        return pending.takeMostRelevantLocation(
            second = potentialUpdate,
            type = performance.balancedPerformanceSettings.config,
            filtering = performance.filtering
        )
    }

    private suspend fun filterByWifi(
        data: LocationUpdateDataFromDevice,
        filtering: LocationFilteringType
    ): LocationUpdateDataFromDevice? = mutex.withLock {
        val wifi = connectedWifi()
        try {
            wifiAnchorMemory.processLocation(wifi = wifi, location = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logg.tag("location_service").w("Error learning wifi location: ${e.message}")
        }

        val isFarFromWifi = wifiAnchorMemory.isFarFromWifi(
            wifi = wifi,
            location = LatLng(lat = data.lat, lng = data.lng),
            accuracyMeters = data.adjustedHorizontalAccuracy
        )

        if (!isFarFromWifi) {
            streak = null
            return@withLock data.copy(isLikelyInaccurate = false)
        }

        val now = nowUTC()
        val previous = streak?.takeIf { now - it.last < 1.hours }
        val current = previous?.copy(count = previous.count + 1, last = now)
            ?: Streak(count = 1, first = now, last = now)
        streak = current

        if (current.count >= filtering.outsideKnownWifiConfirmFixes && now - current.first >= filtering.outsideKnownWifiConfirmMinSpan) {
            // locations keep coming from outside the wifi zone, filter settings allows them
            return@withLock data.copy(isLikelyInaccurate = true)
        }

        logg.tag("location_service")
            .i("Drop location far from wifi (${current.count}) - ${data.timeUTC}")
        null
    }

    private suspend fun connectedWifi(): ConnectedWifi? {
        val now = nowUTC()
        val lastTime = wifiTime
        if (lastTime != null && now - lastTime < 5.seconds) {
            return wifi
        }
        wifi = try {
            connectionStatus.getConnectedWifi()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        wifiTime = now
        return wifi
    }

    private data class Streak(val count: Int, val first: Instant, val last: Instant)
}
