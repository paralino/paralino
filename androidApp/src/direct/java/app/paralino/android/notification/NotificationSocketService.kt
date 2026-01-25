package app.paralino.android.notification

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.paralino.android.remote.sync.FullRefreshSyncWorker
import app.paralino.android.remote.sync.PushNotificationsSyncWorker
import app.paralino.core.logg
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.NetworkConnectionInfo
import app.paralino.core.provider.NotificationConditionStatus
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.RealtimeSubscriptionManager
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class NotificationSocketService : Service() {

    private val serviceDefaultScope = CoroutineScope(Dispatchers.Default + Job())

    private val notificationHandler: NotificationSocketServiceNotificationHandler by inject()
    private val deviceApiProvider: IDeviceApiProvider by inject()
    private val realtimeManager: RealtimeSubscriptionManager by inject()
    private val serviceManager: NotificationSocketServiceManager by inject()

    private var launchedInForeground: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Notification socket service created"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        launchedInForeground = intent?.getBooleanExtra(startedInForeground, false) ?: false

        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Notification socket service starting - action: ${intent?.action} - launched in foreground: $launchedInForeground"
        )

        if (!isServiceActive || deviceApiProvider.appLifecycleState.value.current == AppLifecycleType.ACTIVE) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this,
                        notificationHandler.notificationId,
                        notificationHandler.notify(
                            context = this,
                            errors = getServiceErrors()
                        ),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    ServiceCompat.startForeground(
                        this,
                        notificationHandler.notificationId,
                        notificationHandler.notify(
                            context = this,
                            errors = getServiceErrors()
                        ),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                    )
                }
                logg.v(
                    tag = "notification_socket_service_${this.hashCode()}",
                    message = "Notification socket started in foreground - action: ${intent?.action}"
                )
            } catch (e: Exception) {
                val wrappedError = NotificationSocketServiceCanNotStartException(
                    cause = e,
                    locationConditionStatus = deviceApiProvider.locationStatusProvider.currentStatus,
                    notificationConditionStatus = deviceApiProvider.notificationStatusProvider.currentStatus,
                    connectionStatus = deviceApiProvider.connectionStatusProvider.currentStatus
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                    logg.w(
                        tag = "notification_socket_service_${this.hashCode()}",
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
        } else if (NotificationSocketServiceLauncher.isPackageBootAction(intent?.action)) {
            serviceDefaultScope.coroutineContext.cancelChildren()
            initTracking()
        }

        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Notification socket service started - action: ${intent?.action} - launched in foreground: $launchedInForeground"
        )

        isServiceActive = true

        return START_STICKY
    }

    private fun initTracking() {
        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Subscribe to push notifications"
        )

        serviceManager.listenForServiceStatus()
            .onEach { isEnabled ->
                if (!isEnabled) {
                    logg.v(
                        tag = "notification_socket_service_${this.hashCode()}",
                        message = "Notification socket service can not run, stopping service"
                    )
                    stopAll()
                }
            }
            .launchIn(serviceDefaultScope)

        deviceApiProvider.connectionStatusProvider.status
            .map { it.isOnline }
            .distinctUntilChanged()
            .onEach { isOnline ->
                if (isOnline) {
                    delay(2000)
                    realtimeManager.startNotificationsSubscription()
                    PushNotificationsSyncWorker.createRequest(applicationContext)
                } else {
                    delay(1000)
                    realtimeManager.cancelNotificationsSubscription()
                }
                updateNotification()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun updateNotification() {
        notificationHandler.notify(
            context = this,
            errors = getServiceErrors()
        )
    }

    private fun getServiceErrors(): List<ConditionErrorType> {
        return buildList {
            if (!deviceApiProvider.connectionStatusProvider.status.value.isOnline) {
                add(ConditionErrorType.Network)
            }
        }
    }

    private fun stopAll() {
        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Stop all - notification socket service"
        )
        FullRefreshSyncWorker.cancelWork(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceActive = false
        stopSelf()
    }

    override fun onDestroy() {
        logg.v(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "onDestroy - Killing notification socket service"
        )
        isServiceActive = false
        serviceDefaultScope.cancel()
        notificationHandler.removeNotification()
        realtimeManager.cancelNotificationsSubscription()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isServiceActive = false
        const val startedInForeground = "startedInForeground"

        fun start(
            context: Context,
            appForeground: Boolean,
            action: String?
        ) {
            val intent = Intent(context, NotificationSocketService::class.java)
            intent.putExtra(startedInForeground, appForeground)
            intent.action = action
            ContextCompat.startForegroundService(context, intent)
        }
    }

    class NotificationSocketServiceCanNotStartException(
        override val cause: Throwable?,
        val locationConditionStatus: LocationConditionStatus,
        val notificationConditionStatus: NotificationConditionStatus,
        val connectionStatus: NetworkConnectionInfo,
    ) : Exception() {

        override val message: String?
            get() = buildString {
                cause?.message?.let { append(it) }
                append("\n")
                append(locationConditionStatus.toString())
                append("\n")
                append("connectionStatus: ${connectionStatus.isOnline}")
                append("\n")
                append("notificationPermission: ${notificationConditionStatus.isNotificationPermissionAllowed}")
            }
    }
}