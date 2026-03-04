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
import app.paralino.core.models.data
import app.paralino.core.models.user.PushProviderEndpointConfig
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.NetworkConnectionInfo
import app.paralino.core.provider.NotificationConditionStatus
import app.paralino.core.provider.NotificationSocketServiceManager
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

    private val notificationHandler: PushServiceNotificationHandler by inject()
    private val deviceApiProvider: IDeviceApiProvider by inject()
    private val webSocketManager: PushServiceWebSocketManager by inject()
    private val personalSpaceLocal: app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource by inject()
    private val serviceManager: NotificationSocketServiceManager by inject()
    
    private var websocketState: PushServiceWebSocketConnection.WebSocketConnectionState? = null

    private var launchedInForeground: Boolean = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        logg.i(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Notification socket service created"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        launchedInForeground = intent?.getBooleanExtra(startedInForeground, false) ?: false

        logg.i(
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
                            errors = getServiceErrors(),
                            connectionState = websocketState
                        ),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    ServiceCompat.startForeground(
                        this,
                        notificationHandler.notificationId,
                        notificationHandler.notify(
                            context = this,
                            errors = getServiceErrors(),
                            connectionState = websocketState
                        ),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                    )
                }
                logg.i(
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

        logg.i(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Notification socket service started - action: ${intent?.action} - launched in foreground: $launchedInForeground"
        )

        isServiceActive = true

        return START_STICKY
    }

    private fun initTracking() {
        logg.i(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Subscribe to push notifications"
        )

        webSocketManager.connectionState
            .onEach { state ->
                websocketState = state
                updateNotification()
            }
            .launchIn(serviceDefaultScope)

        serviceManager.listenForServiceStatus()
            .onEach { isEnabled ->
                if (!isEnabled) {
                    logg.i(
                        tag = "notification_socket_service_${this.hashCode()}",
                        message = "Notification socket service can not run, stopping service"
                    )
                    stopAll()
                }
            }
            .launchIn(serviceDefaultScope)

        personalSpaceLocal.personalSpaceState
            .map { it.data?.currentDevice?.pushProviderConfig }
            .distinctUntilChanged()
            .onEach { configString ->
                val config = PushProviderEndpointConfig.get(configString)
                if (config == null || config.distributor != "app.paralino.android") {
                    logg.i(
                        tag = "notification_socket_service_${this.hashCode()}",
                        message = "Websocket config not valid, cancel connection"
                    )
                    webSocketManager.closeConnection()
                    websocketState = PushServiceWebSocketConnection.WebSocketConnectionState.Disconnected
                } else {
                    logg.i(
                        tag = "notification_socket_service_${this.hashCode()}",
                        message = "Websocket config valid, start connection"
                    )
                    websocketState = webSocketManager.connectionState.value
                    if (deviceApiProvider.connectionStatusProvider.status.value.isOnline) {
                        webSocketManager.startConnection(config.url)
                    }
                }
                updateNotification()
            }
            .launchIn(serviceDefaultScope)

        deviceApiProvider.connectionStatusProvider.status
            .map { it.isOnline }
            .distinctUntilChanged()
            .onEach { isOnline ->
                if (isOnline) {
                    delay(2000)
                    PushNotificationsSyncWorker.createRequest(applicationContext)
                    val configString = personalSpaceLocal.personalSpaceState.value.data?.currentDevice?.pushProviderConfig
                    val config = PushProviderEndpointConfig.get(configString)
                    if (config != null && config.distributor == "app.paralino.android") {
                        logg.i(
                            tag = "notification_socket_service_${this.hashCode()}",
                            message = "Device online, websocket config valid, start connection"
                        )
                        webSocketManager.startConnection(config.url)
                    } else {
                        logg.i(
                            tag = "notification_socket_service_${this.hashCode()}",
                            message = "Device online, websocket config not valid, cancel connection"
                        )
                    }
                } else {
                    delay(1000)
                    logg.i(
                        tag = "notification_socket_service_${this.hashCode()}",
                        message = "Device offline, cancel connection"
                    )
                    webSocketManager.closeConnection()
                }
                updateNotification()
            }
            .launchIn(serviceDefaultScope)
    }

    private fun updateNotification() {
        notificationHandler.notify(
            context = this,
            errors = getServiceErrors(),
            connectionState = websocketState
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
        logg.i(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "Stop all - notification socket service"
        )
        FullRefreshSyncWorker.cancelWork(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceActive = false
        stopSelf()
    }

    override fun onDestroy() {
        logg.i(
            tag = "notification_socket_service_${this.hashCode()}",
            message = "onDestroy - Killing notification socket service"
        )
        isServiceActive = false
        serviceDefaultScope.cancel()
        notificationHandler.removeNotification()
        webSocketManager.closeConnection()
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