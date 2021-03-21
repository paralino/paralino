package com.zbapp.locationtrack.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.data
import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.location.repo.location.LocationRepository
import com.zbapp.locationtrack.data.models.location.LocationEntity
import com.zbapp.locationtrack.data.models.location.LocationMapper
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import loge
import logv
import logw
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class LocationService: Service() {

    private var locationJob: Job? = null
    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val notificationHandler: LocationNotificationHandler by inject()
    private val locationInitHandler: LocationInitHandler by inject()
    private val locationRepository: LocationRepository by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(notificationHandler.notificationId, notificationHandler.notify(this))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isServiceActive = true
        initTracking()
        return Service.START_REDELIVER_INTENT
    }

    private fun initTracking() {
        if(LocationConditionStatus.isOk(this)) {
            locationRepository.sharingManager.sharingStatus
                .onEach {
                    logv("Location sharing status: $it")
                    if(it) startTracking()
                    else stopAll()
                }
                .catch {
                    loge(it)
                    stopAll()
                }
                .launchIn(serviceDefaultScope)
        } else {
            logv("Location conditions not met")
            stopAll()
        }
    }

    private fun startTracking() {
        locationInitHandler.startLocationUpdates(mainLooper)
        locationJob?.cancel()
        locationJob = locationInitHandler.currentLocation
            .onEach {
                it.data?.let { saveLocation(it) }
                logv("Got location: $it")
            }
            .launchIn(serviceDefaultScope)
    }

    private fun saveLocation(location: LocationEntity) {
        if(locationRepository.sharingManager.isSharingActive) {
            locationRepository.pushLocation(LocationMapper.map(location))
                .catch { logw("Error saving saving location") }
                .launchIn(serviceDefaultScope)
        } else {
            // We can stop service since all sharing contracts are expired
            stopAll()
        }
    }

    private fun stopAll() {
        logv("Stop all - location service")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        logv("Killing location service")
        locationInitHandler.stopLocationUpdates()
        notificationHandler.removeNotification()
        isServiceActive = false
        locationJob?.cancel()
        serviceDefaultScope.cancel()
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