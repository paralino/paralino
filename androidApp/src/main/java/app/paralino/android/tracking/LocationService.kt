package app.paralino.android.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import app.paralino.android.AppClass
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.core.domain.location.RealtimeEventsWhileSharingUseCase
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import loge
import logv
import logw
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime


class LocationService : Service() {

    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val notificationHandler: LocationNotificationHandler by inject()
    private val locationInitHandler: LocationInitHandler by inject()
    private val locationSharingManager: LocationSharingManager by inject()
    private val locationRepository: LocationRepository by inject()
    private val sharingRealtimeEvents: RealtimeEventsWhileSharingUseCase by inject()
    private val locationStatus: ILocationStatusProvider by inject()
    private val placesRepository: PlacesRepository by inject()
    private val geofenceManager: IGeofenceManager by inject()

    private var launchedInForeground: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logv(this.hashCode().toString(), "Location service created")
        startForeground(
            notificationHandler.notificationId,
            notificationHandler.notify(this, groupCount = 0, emptyList())
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        logv(this.hashCode().toString(), "Location service started - action: ${intent.action}")
        launchedInForeground = intent.getBooleanExtra(startedInForeground, false)
        isServiceActive = true
        serviceDefaultScope.coroutineContext.cancelChildren()
        initTracking()
        return Service.START_REDELIVER_INTENT
    }

    private fun initTracking() {
        logv(this.hashCode().toString(), "Subscribe to location and sharing status")

        (application as AppClass).locationStatus.status
            .onEach {
                if (it.isOK()) {
                    locationInitHandler.startLocationUpdates()
                } else {
                    locationInitHandler.stopLocationUpdates()
                }
                updateNotification()
            }
            .launchIn(serviceDefaultScope)

        locationSharingManager.allContractsFlow
            .onEach {
                logv(this.hashCode().toString(), "Location sharing status: $it")
                if (it is Resource.Ready) {
                    if (it.data.isNotEmpty()) {
                        startTracking()
                    } else {
                        stopAll()
                    }
                }
            }
            .catch {
                loge(it)
                stopAll()
            }
            .launchIn(serviceDefaultScope)

        listenForPlaces()
    }

    private fun updateNotification() {
        val errors = (application as AppClass).locationStatus.status.value.getPossibleErrors(
            forBackground = !launchedInForeground
        )
        val count = locationSharingManager.allContractsFlow.value.data?.size ?: 0
        notificationHandler.notify(this, count, errors)
    }

    private fun startTracking() {
        updateNotification()

        logv(this.hashCode().toString(), "Subscribing to location updates")

        locationInitHandler.currentLocation
            .onEach {
                logv(this.hashCode().toString(), "Got location: $it")
                it.data?.let {
                    launchedInForeground = true
                    updateNotification()
                    saveLocation(it)
                }
            }
            .launchIn(serviceDefaultScope)

        sharingRealtimeEvents.listenForEventsWhileSharing()
            .launchIn(serviceDefaultScope)
    }

    private fun saveLocation(data: LocationDataModel) {
        flow {
            locationRepository.pushLocation(data)
            emit(Unit)
        }.catch {
            logw("Error saving saving location")
            if (it is NoValidContractsException) {
                // We can stop service since all sharing contracts are expired
                stopAll()
            }
        }.launchIn(serviceDefaultScope)
    }

    @OptIn(ExperimentalTime::class)
    private fun listenForPlaces() {

        // timer to reregister places after certain time
        val timer = flow {
            while (currentCoroutineContext().isActive) {
                emit(Unit)
                delay(20L.hours.inWholeMilliseconds)
            }
        }

        val places = placesRepository.getAllActivePlaces()
            .map {
                it.map {
                    GroupPlaceGeofenceData(
                        placeId = it.id,
                        lat = it.data.lat,
                        lng = it.data.lng,
                        radiusMeters = it.data.radiusMeters
                    )
                }
            }
            .distinctFlowOfList()

        combine(places, locationStatus.status, timer) { geofences, locationStatus, _ ->
            if (locationStatus.isOK(forAlwaysOnAccess = true)) {
                geofenceManager.registerGeofences(geofences)
            } else {
                geofenceManager.deleteAllGeofences()
            }
        }
            .catch {
                // delete all geofences when coroutine is canceled
                if(it is CancellationException)
                    geofenceManager.deleteAllGeofences()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun stopAll() {
        logv(this.hashCode().toString(), "Stop all - location service")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        logv(this.hashCode().toString(), "onDestroy - Killing location service")
        isServiceActive = false
        serviceDefaultScope.cancel()
        locationInitHandler.stopLocationUpdates()
        notificationHandler.removeNotification()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isServiceActive = false
        const val startedInForeground = "startedInForeground"

        fun start(context: Context, appForeground: Boolean) {
            val intent = Intent(context, LocationService::class.java)
            intent.putExtra(startedInForeground, appForeground)
            ContextCompat.startForegroundService(context, intent)
        }
    }

}