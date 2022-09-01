package app.paralino.core.repo.location

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.config
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LocationPerformanceManager constructor(
    private val realtimeApi: IRealtimeApi,
    private val documentApi: IDocumentsApi,
    private val locationSharingManager: LocationSharingManager,
    val scope: ICoroutineScopeProvider,
) : BaseRepository() {

    private val _locationPerformanceType = MutableStateFlow(LocationPerformanceType.BALANCED)
    val locationPerformanceType: StateFlow<LocationPerformanceType> = _locationPerformanceType
    private var performanceUpdateJob: Job? = null

    suspend fun boostOnMap(groupId: String) {
        _locationPerformanceType.value = LocationPerformanceType.HIGH
        schedulePerformanceReset(resetDelay = 5_000)
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


    fun listenForActiveUsers(): Flow<Unit> {
        val event =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionLocationFocusID}.documents"
        return realtimeApi.subscribeWithFilter(
            channel = event,
            filters = listOf("$event.*.create", "$event.*.update")
        )
            .onEach {
                val groupId = it["groupId"] as String
                if (locationSharingManager.isSharingActiveForGroup(groupId)) {
                    _locationPerformanceType.value = LocationPerformanceType.HIGH
                    schedulePerformanceReset()
                }
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