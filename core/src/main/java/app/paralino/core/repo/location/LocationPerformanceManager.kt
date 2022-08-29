package app.paralino.core.repo.location

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LocationPerformanceManager constructor(
    private val realtimeApi: IRealtimeApi,
    val scope: ICoroutineScopeProvider,
) {

    private val _locationPerformanceType = MutableStateFlow(LocationPerformanceType.BALANCED)
    val locationPerformanceType: StateFlow<LocationPerformanceType> = _locationPerformanceType
    private var performanceUpdateJob: Job? = null

    fun boostOnMap() {
        _locationPerformanceType.value = LocationPerformanceType.HIGH
        schedulePerformanceReset(resetDelay = 5_000)
    }

    fun listenForActiveUsers(): Flow<Unit> {
        return realtimeApi.subscribe("databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents")
            .onEach {
                _locationPerformanceType.value = LocationPerformanceType.HIGH
                schedulePerformanceReset()
            }
            .distinctUntilChanged()
            .map { }
    }

    private fun schedulePerformanceReset(resetDelay: Long = 10_000) {
        performanceUpdateJob?.cancel()
        performanceUpdateJob = scope.userSessionIOScope.launch {
            delay(resetDelay)
            _locationPerformanceType.value = LocationPerformanceType.BALANCED
        }
    }

}