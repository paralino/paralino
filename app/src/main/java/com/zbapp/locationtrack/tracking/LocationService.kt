package com.zbapp.locationtrack.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.zbapp.core.models.Payload
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import loge
import logv
import logw
import org.koin.android.ext.android.inject

class LocationService : Service() {

    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val notificationHandler: LocationNotificationHandler by inject()
    private val locationInitHandler: LocationInitHandler by inject()
    private val locationRepository: LocationRepository by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logv(this.hashCode().toString(), "Location service created")
        startForeground(notificationHandler.notificationId, notificationHandler.notify(this))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        logv(this.hashCode().toString(), "Location service started - action: ${intent.action}")
        isServiceActive = true
        serviceDefaultScope.coroutineContext.cancelChildren()
        initTracking()
        return Service.START_REDELIVER_INTENT
    }

    private fun initTracking() {
        logv(this.hashCode().toString(), "Subscribe to location and sharing status")

        (application as AppClass).locationStatus
            .onEach {
                if(it.isOK()) {
                    locationInitHandler.startLocationUpdates()
                } else {
                    locationInitHandler.stopLocationUpdates()
                }
                notificationHandler.notify(this, it.toString())
            }
            .launchIn(serviceDefaultScope)

        locationRepository.sharingManager.sharingStatus
            .onEach {
                logv(this.hashCode().toString(), "Location sharing status: $it")
                if (it is Resource.Ready) {
                    if(it.data)
                        startTracking()
                    else stopAll()
                }
            }
            .catch {
                loge(it)
                stopAll()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun startTracking() {
        logv(this.hashCode().toString(), "Subscribing to location updates")
        locationInitHandler.currentLocation
            .onEach {
                logv(this.hashCode().toString(), "Got location: $it")
                it.data?.let { saveLocation(it) }
            }
            .launchIn(serviceDefaultScope)
    }

    private fun saveLocation(data: LocationDataModel) {
        locationRepository.pushLocation(data)
            .catch {
                logw("Error saving saving location")
                if (it is NoValidContractsException) {
                    // We can stop service since all sharing contracts are expired
                    stopAll()
                }
            }
            .launchIn(serviceDefaultScope)
    }

    private fun stopAll() {
        logv(this.hashCode().toString(), "Stop all - location service")
        stopForeground(true)
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

        fun start(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

}