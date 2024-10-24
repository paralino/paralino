package app.paralino.android.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.remote.sync.FullRefreshSyncWorker
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LocationService : Service() {

    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val notificationHandler: LocationNotificationHandler by inject()
    private val locationInitHandler: LocationInitHandler by inject()
    private val locationSharingManager: LocationSharingManager by inject()
    private val locationUploadEngine: LocationUploadEngine by inject()
    private val deviceApiProvider: IDeviceApiProvider by inject()

    private var launchedInForeground: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logg.v(this.hashCode().toString(), "Location service created")
        startForeground(
            notificationHandler.notificationId,
            notificationHandler.notify(this, groupCount = 0, errors = emptyList())
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        launchedInForeground = intent.getBooleanExtra(startedInForeground, false)
        logg.v(this.hashCode().toString(), "Location service started - action: ${intent.action} - launched in foreground: $launchedInForeground")
        isServiceActive = true
        serviceDefaultScope.coroutineContext.cancelChildren()
        initTracking()
        return Service.START_REDELIVER_INTENT
    }

    private fun initTracking() {
        logg.v(this.hashCode().toString(), "Subscribe to location and sharing status")

        deviceApiProvider.locationStatusProvider.status
            .onEach {
                if (it.isOK()) {
                    locationInitHandler.startLocationUpdates()
                } else {
                    locationInitHandler.stopLocationUpdates()
                }
            }
            .launchIn(serviceDefaultScope)

        locationSharingManager.allContractsFlow
            .filterIsInstance<Resource.Ready<List<LocationSharingContract>>>()
            .map { it.data.isNotEmpty() }
            .distinctUntilChanged()
            .onEach {
                logg.v(this.hashCode().toString(), "Location sharing status: $it")
                if (it) {
                    startTracking()
                } else {
                    stopAll()
                }
            }
            .catch {
                logg.e("Error start/stop tracking", it)
                stopAll()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun updateNotification() {
        val errors = buildList {
            addAll(
                deviceApiProvider.locationStatusProvider.currentStatus.getPossibleErrors(
                    forBackground = !launchedInForeground
                )
            )
            if(!deviceApiProvider.connectionStatusProvider.status.value.isOnline) {
                add(ConditionErrorType.Network)
            }
        }
        val count = locationSharingManager.allContractsFlow.value.data?.size ?: 0
        notificationHandler.notify(this, count, errors)
    }

    private fun startTracking() {
        updateNotification()

        logg.v(this.hashCode().toString(), "Subscribing to location updates")
        FullRefreshSyncWorker.createRequest(applicationContext)

        locationInitHandler.currentLocation
            .onEach {
                when (val location = it.data) {
                    null -> {
                        logg.v(this.hashCode().toString(), "Got null location")
                    }
                    else -> {
                        launchedInForeground = true
                        updateNotification()
                        saveLocation(location)
                    }
                }
            }
            .launchIn(serviceDefaultScope)

        deviceApiProvider.deviceStatus
            .onEach {
                logg.v("device_status", "Status changed: $it")
                updateNotification()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun saveLocation(data: LocationDataModel) {
        serviceDefaultScope.launch {
            try {
                locationUploadEngine.registerNewLocation(data)
            } catch (e: NoValidContractsException) {
                logg.w("Error saving saving location - sharing expired")
                // We can stop service since all sharing contracts are expired
                stopAll()
            } catch (e: Exception) {
                logg.w("Error saving saving location", e)
            }
        }
    }

    private fun stopAll() {
        logg.v(this.hashCode().toString(), "Stop all - location service")
        FullRefreshSyncWorker.cancelWork(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        logg.v(this.hashCode().toString(), "onDestroy - Killing location service")
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