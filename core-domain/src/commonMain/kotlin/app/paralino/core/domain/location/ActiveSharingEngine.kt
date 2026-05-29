package app.paralino.core.domain.location

import app.paralino.core.domain.location.history.ILocationHistoryUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.AppHost
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ActiveSharingEngine(
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val geofenceRegistrationMonitor: GeofenceRegistrationMonitor,
    private val scopeProvider: ICoroutineScopeProvider,
    private val locationUploadEngine: LocationUploadEngine,
    private val locationHistoryUploadEngine: ILocationHistoryUploadEngine,
    private val locationCache: LocationCacheRepository,
    private val appConfig: AppConfigRepository
): BaseRepository() {

    private var activePingJob: Job? = null
    private var sharingStatusJob: Job? = null
    private var deviceStatusJob: Job? = null

    fun startMonitoring() {
        deviceStatusJob?.cancel()
        sharingStatusJob?.cancel()

        when (appConfig.appHost) {
            AppHost.MAIN_APP -> {
                deviceStatusJob = combine(
                    deviceApiProvider.deviceStatus,
                    locationPerformanceManager.locationPerformanceType
                        .filter { it.current <= LocationPerformanceType.BALANCED }
                        .map { it.current }
                        .distinctUntilChanged()
                ) { deviceStatus, locationPerformance ->
                    if (locationSharingManager.isSharingActive()) {
                        logg.v("location_service", "Sending location status due to device status change")
                        updateStatus()
                    }
                }
                    .launchIn(scopeProvider.defaultIOScope)

                sharingStatusJob = locationSharingManager.sharingStatus
                    .mapNotNull { it.data }
                    .onEach {
                        if (it) {
                            sendStatusPeriodically()
                        } else {
                            logg.i("location_service", "Cancel periodic location status update")
                            activePingJob?.cancel()
                            locationHistoryUploadEngine.syncPendingEntriesInBackground(cacheTime = 0.seconds)
                        }
                    }
                    .launchIn(scopeProvider.defaultIOScope)

                geofenceRegistrationMonitor.startGeofenceRegistration(reRegisterPeriodically = true)
                locationUploadEngine.startLocationUploadEngine()
            }

            AppHost.IOS_NOTIFICATION_EXTENSION -> {
                // do nothing
            }
            AppHost.IOS_LOCATION_EXTENSION -> {
                // do nothing
            }
        }
    }

    private fun sendStatusPeriodically() {
        logg.d("location_service", "Start periodic location status update")
        activePingJob?.cancel()
        activePingJob = scopeProvider.userSessionIOScope.launch {
            val batteryCheck = async {
                while (isActive) {
                    delay(20.minutes)
                    if (deviceApiProvider.batteryLevel?.takeIf { it < BatteryNotificationsSettingsData.LOW_BATTERY_LEVEL } != null) {
                        logg.d("location_service", "Sending location status for low battery")
                        updateStatus()
                    } else {
                        logg.v("location_service", "Skip sending location status for low battery")
                    }
                }
            }
            val locationCheck = async {
                while (isActive) {
                    delay(2.hours)
                    if (locationCache.lastLocation?.isOlderThan(3.hours) != false) {
                        logg.d("location_service", "Sending location status after inactivity")
                        updateStatus()
                    } else {
                        logg.v("location_service", "Skip sending location status after inactivity")
                    }
                }
            }

            awaitAll(batteryCheck, locationCheck)
        }
    }

    private fun updateStatus() {
        scopeProvider.userSessionIOScope.launch {
            try {
                locationUploadEngine.updateDeviceStatusOnly(
                    tryToFetchCachedLocation = true
                )
            } catch (e: Exception) {
                logg.w("Error updating device status: ${e.message}")
            }
        }
    }
}