package app.paralino.core.domain.location.engine

import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

abstract class ILocationEventProcessingEngine(
    private val scope: CoroutineScope,
): BaseRepository() {
    private var eventMonitoringJob: Job? = null
    private var lastEventTime = MutableStateFlow<Instant?>(null)

    abstract val tag: String
    abstract val processingRate: Duration
    protected abstract fun eventAction(locationData: LocationDataModel)

    fun start(
        pendingLocationFlow: StateFlow<LocationDataModel?>
    ) {
        eventMonitoringJob?.cancel()
        eventMonitoringJob = scope.launch {
            logg.d(tag = "location_service", message = "Start location event $tag processing job")
            try {
                pendingLocationFlow.filterNotNull()
                    .conflate()
                    .collectLatest { pendingLocation ->
                        logg.d(tag = "location_service", message = "Processing new location for $tag")
                        val lastUpdate = lastEventTime.value
                        val diff = if (lastUpdate != null) nowUTC() - lastUpdate else 0.seconds

                        val processingRate = processingRate
                        if (diff > Duration.ZERO && diff < processingRate) {
                            val delay = processingRate - diff
                            // no need to lock thread if potential delay is small
                            if (delay > 100.milliseconds) {
                                logg.d(
                                    tag = "location_service",
                                    message = "Delay location $tag by $delay"
                                )
                                delay(delay)
                            }
                        }

                        logg.d(
                            tag = "location_service",
                            message = "Location (${pendingLocation.timeUTC}) ready for $tag"
                        )
                        lastEventTime.update { nowUTC() }
                        eventAction(pendingLocation)
                    }
            } catch (e: Exception) {
                logg.w(
                    tag = "location_service",
                    message = "Error processing pending location for $tag: ${e.message}",
                    t = e
                )
            }
        }
    }

    fun stop() {
        eventMonitoringJob?.cancel()
        eventMonitoringJob = null
        lastEventTime.update { null }
    }
}