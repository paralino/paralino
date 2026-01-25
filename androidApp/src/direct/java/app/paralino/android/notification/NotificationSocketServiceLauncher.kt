package app.paralino.android.notification

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Build
import app.paralino.core.logg
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class NotificationSocketServiceLauncher(
    private val appClass: Application,
    private val deviceApiProvider: IDeviceApiProvider,
    private val infoMessagesNotifications: InfoMessagesNotificationPresenter,
    private val serviceManager: NotificationSocketServiceManager,
    private val appConfigRepo: AppConfigRepository,
    private val scopeProvider: ICoroutineScopeProvider
) {
    private val _pendingNotificationsChannel = Channel<Boolean?>()
    private var lastNotificationPost: Instant? = null

    init {
        scopeProvider.defaultIOScope.launch {
            try {
                _pendingNotificationsChannel
                    .receiveAsFlow()
                    .conflate()
                    .collectLatest { backgroundLocationError ->
                        delay(1000)
                        if (backgroundLocationError != null) {
                            postNotificationMaybe(backgroundLocationError = backgroundLocationError)
                        }
                    }
            } catch (e: Exception) {
            }
        }
    }

    fun startMonitoring(scope: CoroutineScope) {
        if (appConfigRepo.hasNotificationSocketService) {
            combine(
                deviceApiProvider.appLifecycleState,
                serviceManager.listenForServiceStatus(),
            ) { appState, serviceStatus ->
                Pair(appState, serviceStatus)
            }
                .debounce(500)
                .onEach { (appState, serviceStatus) ->
                    if (appState.current == AppLifecycleType.ACTIVE && serviceStatus) {
                        startNotificationService(
                            action = NotificationSocketServiceLauncher.INTENT_NOTIFICATION_SOCKET_SERVICE_START
                        )
                    }
                }
                .launchIn(scope)
        } else {
            scope.launch(Dispatchers.IO) {
                serviceManager.setServiceEnabledState(enabledStatus = false)
            }
        }
    }

    fun startNotificationService(
        action: String?
    ) {
        scopeProvider.userSessionIOScope.launch {
            val isServiceEnabled = serviceManager.isServiceEnabledInSettings()

            if (isServiceEnabled) {
                val areNotificationsEnabled = deviceApiProvider.notificationStatusProvider.currentStatus.isNotificationPermissionAllowed
                if (areNotificationsEnabled) {
                    if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
                        // here we already have some location permission, but GPS might still be off
                        startLocationService(isForeground = true, action = action)
                    } else {
                        startLocationService(isForeground = false, action = action)
                    }
                } else {
                    logg.w(
                        tag = "notification_socket_service",
                        message = "Notification socket service missing notification permissions for $action"
                    )
                }
            } else {
                logg.w(
                    tag = "notification_socket_service",
                    message = "Notification socket service disabled by user, skip starting it."
                )
            }
        }
    }


    private fun postNotificationMaybe(backgroundLocationError: Boolean) {
        if (lastNotificationPost?.isOlderThan(10.minutes) != false) {
            lastNotificationPost = nowUTC()
            infoMessagesNotifications.notifyNotificationSocketServiceError(
                backgroundStartError = backgroundLocationError,
                context = appClass
            )
        }
    }

    private fun startLocationService(
        isForeground: Boolean,
        action: String?
    ) {
        try {
            logg.w(
                tag = "notification_socket_service",
                message = "Starting notification socket service from $action"
            )
            NotificationSocketService.start(
                context = appClass,
                appForeground = isForeground,
                action = action
            )
            _pendingNotificationsChannel.trySend(null)
        } catch (e: Exception) {
            logg.e(
                tag = "notification_socket_service",
                message = "Can not start notification socket service from $action",
                t = e
            )
            infoMessagesNotifications.notifyNotificationSocketServiceError(
                backgroundStartError = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException,
                context = appClass
            )
        }
    }

    companion object {
        const val INTENT_NOTIFICATION_SOCKET_SERVICE_START = "app.paralino.android.NOTIFICATION_SOCKET_SERVICE_START"
        fun isPackageBootAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
        }
    }
}