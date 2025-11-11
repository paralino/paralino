package app.paralino.android.tracking

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.remote.sync.FullRefreshSyncWorker
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.NotificationConditionStatus
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
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
        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Location service created"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        launchedInForeground = intent?.getBooleanExtra(startedInForeground, false) ?: false

        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Location service starting - action: ${intent?.action} - launched in foreground: $launchedInForeground"
        )

        if (!isServiceActive || deviceApiProvider.appLifecycleState.value.current == AppLifecycleType.ACTIVE) {
            try {
                ServiceCompat.startForeground(
                    this,
                    notificationHandler.notificationId,
                    notificationHandler.notify(
                        context = this,
                        groupCount = 0,
                        isPersonalSpaceActive = false,
                        errors = emptyList()
                    ),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
                logg.v(
                    tag = "location_service_${this.hashCode()}",
                    message = "Location service started in foreground - action: ${intent?.action}"
                )
            } catch (e: Exception) {
                val wrappedError = LocationServiceCanNotStartException(
                    cause = e,
                    locationConditionStatus = deviceApiProvider.locationStatusProvider.currentStatus,
                    notificationConditionStatus = deviceApiProvider.notificationStatusProvider.currentStatus,
                    alwaysOnSharing = locationSharingManager.allContractsFlow.value.data?.any { it.isAlwaysOn }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                    logg.e(
                        tag = "location_service_${this.hashCode()}",
                        message = "Unable to start as foreground service due to ForegroundServiceStartNotAllowedException",
                        t = wrappedError
                    )
                } else {
                    throw wrappedError
                }
            }
        }

        if (!isServiceActive) {
            serviceDefaultScope.coroutineContext.cancelChildren()
            initTracking()
        } else if (LocationServiceLauncher.isPackageBootAction(intent?.action)) {
            serviceDefaultScope.coroutineContext.cancelChildren()
            initTracking()
        }

        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Location service started - action: ${intent?.action} - launched in foreground: $launchedInForeground"
        )

        isServiceActive = true

        return START_STICKY
    }

    private fun initTracking() {
        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Subscribe to location and sharing status"
        )

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
                logg.v(
                    tag = "location_service_${this.hashCode()}",
                    message = "Location sharing status: $it"
                )
                if (it) {
                    startTracking()
                } else {
                    stopAll()
                }
            }
            .catch {
                logg.e(
                    tag = "location_service_${this.hashCode()}",
                    message = "Error start/stop tracking",
                    t = it
                )
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
            if (!deviceApiProvider.connectionStatusProvider.status.value.isOnline) {
                add(ConditionErrorType.Network)
            }
        }
        val groupCount = locationSharingManager.allContractsFlow.value.data?.count { !it.groupOrSpaceId.isPersonalSpace } ?: 0
        val isPersonalSpaceActive = locationSharingManager.isSharingActiveForGroupMaybe(id = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
        notificationHandler.notify(
            context = this,
            groupCount = groupCount,
            isPersonalSpaceActive = isPersonalSpaceActive,
            errors = errors
        )
    }

    private fun startTracking() {
        updateNotification()

        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Subscribing to location updates"
        )
        FullRefreshSyncWorker.createRequest(applicationContext)

        locationInitHandler.currentLocation
            .onEach {
                when (val location = it.data) {
                    null -> {
                        logg.v(
                            tag = "location_service_${this.hashCode()}",
                            message = "Got null location"
                        )
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
                logg.v(tag = "device_status", message = "Status changed: $it")
                updateNotification()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun saveLocation(data: LocationDataModel) {
        serviceDefaultScope.launch {
            try {
                locationUploadEngine.registerNewLocation(data)
            } catch (e: NoValidContractsException) {
                logg.w(
                    tag = "location_service_${this.hashCode()}",
                    message = "Error saving saving location - sharing expired"
                )
                // We can stop service since all sharing contracts are expired
                stopAll()
            } catch (e: Exception) {
                logg.w(
                    tag = "location_service_${this.hashCode()}",
                    message = "Error saving saving location",
                    t = e
                )
            }
        }
    }

    private fun stopAll() {
        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "Stop all - location service"
        )
        FullRefreshSyncWorker.cancelWork(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceActive = false
        stopSelf()
    }

    override fun onDestroy() {
        logg.v(
            tag = "location_service_${this.hashCode()}",
            message = "onDestroy - Killing location service"
        )
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
        const val INTENT_ACTION_LOCATION_SERVICE_START = "app.paralino.android.LOCATION_SERVICE_START"

        fun start(
            context: Context,
            appForeground: Boolean,
            action: String?
        ) {
            val intent = Intent(context, LocationService::class.java)
            intent.putExtra(startedInForeground, appForeground)
            intent.action = action
            ContextCompat.startForegroundService(context, intent)
        }
    }

    class LocationServiceCanNotStartException(
        override val cause: Throwable?,
        val locationConditionStatus: LocationConditionStatus,
        val notificationConditionStatus: NotificationConditionStatus,
        val alwaysOnSharing: Boolean?
    ) : Exception() {

        override val message: String?
            get() = buildString {
                cause?.message?.let { append(it) }
                append("\n")
                append(locationConditionStatus.toString())
                append("\n")
                append("alwaysOnSharing: $alwaysOnSharing")
                append("\n")
                append("notificationPermission: ${notificationConditionStatus.isNotificationPermissionAllowed}")
            }
    }
}