package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class ActiveSharingEngine(
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val geofenceRegistrationMonitor: GeofenceRegistrationMonitor,
    private val scopeProvider: ICoroutineScopeProvider,
    private val locationRepo: LocationUploadEngine,
    private val locationCache: LocationCacheRepository
): BaseRepository() {

    private var activePingJob: Job? = null

    fun startMonitoring() {
        combine(
            deviceApiProvider.deviceStatus,
            locationPerformanceManager.locationPerformanceType
                .filter { it.current <= LocationPerformanceType.BALANCED }
                .distinctUntilChanged()
        ) { deviceStatus, locationPerformance ->
            if (locationSharingManager.isSharingActive()) {
                logg.v("location_service", "Sending location status due to device status change")
                updateStatus()
            }
        }
            .launchIn(scopeProvider.defaultIOScope)

        locationSharingManager.sharingStatus
            .mapNotNull { it.data }
            .onEach {
                if (it) {
                    sendStatusPeriodically()
                } else {
                    logg.v("location_service", "Cancel periodic location status update")
                    activePingJob?.cancel()
                }
            }
            .launchIn(scopeProvider.defaultIOScope)

        geofenceRegistrationMonitor.startGeofenceRegistration(reRegisterPeriodically = true)
        locationRepo.startLocationUploadEngine()
    }

    private fun sendStatusPeriodically() {
        logg.v("location_service", "Start periodic location status update")
        activePingJob?.cancel()
        activePingJob = scopeProvider.userSessionIOScope.launch {
            while (isActive) {
                delay(5.minutes)
                if (locationCache.lastLocation?.isOlderThan(5.minutes) != false) {
                    logg.v("location_service", "Sending location status after inactivity")
                    updateStatus()
                } else {
                    logg.v("location_service", "Skip sending location status after inactivity")
                }
            }
        }
    }

    private fun updateStatus() {
        scopeProvider.userSessionIOScope.launch {
            try {
                locationRepo.updateDeviceStatusOnly()
            } catch (e: Exception) {
                logg.w("Error updating device status: ${e.message}")
            }
        }
    }
}