package app.paralino.android.tracking

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Build
import app.paralino.android.notification.InfoMessagesNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes

class LocationServiceLauncher(
    private val appClass: Application,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationSharingManager: LocationSharingManager,
    private val infoMessagesNotifications: InfoMessagesNotificationPresenter,
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

    fun startLocationService(
        action: String?
    ) {
        scopeProvider.userSessionIOScope.launch {
            if (locationSharingManager.isSharingActive()) {
                val status = deviceApiProvider.locationStatusProvider.currentStatus
                val hasPermissionForBackground = status.isOK(forAlwaysOnAccess = true)
                val hasPermissionForForeground = status.isOK(forAlwaysOnAccess = false)
                val hasAnyPermission = status.areOnlyPermissionsOk(forAlwaysOnAccess = false)

                if (!hasAnyPermission) {
                    logg.w(
                        tag = "location_service",
                        message = "Location sharing missing permissions for $action"
                    )

                    if (isPackageBootAction(action)) {
                        postNotificationMaybe(backgroundLocationError = true)
                    } else {
                        _pendingNotificationsChannel.trySend(true)
                    }
                } else if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
                    // here we already have some location permission, but GPS might still be off
                    startLocationService(isForeground = true, action = action)

                    if (!hasPermissionForForeground) {
                        logg.w(
                            tag = "location_service",
                            message = "App in foreground but missing location permissions for $action"
                        )
                        _pendingNotificationsChannel.trySend(true)
                    }
                } else {
                    if (hasPermissionForBackground) {
                        startLocationService(isForeground = false, action = action)
                    } else {
                        // Android <= 11, we can start foreground service
                        // but location data is not accessible
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                            startLocationService(isForeground = false, action = action)
                        }

                        logg.w(
                            tag = "location_service",
                            message = "App in background but missing background permissions for $action"
                        )
                        if (isPackageBootAction(action)) {
                            postNotificationMaybe(backgroundLocationError = true)
                        } else {
                            _pendingNotificationsChannel.trySend(true)
                        }
                    }
                }
            } else {
                logg.v("location_service", "Sharing not active, skip location service")
            }
        }
    }

    private fun postNotificationMaybe(backgroundLocationError: Boolean) {
        if (lastNotificationPost?.isOlderThan(10.minutes) != false) {
            lastNotificationPost = nowUTC()
            infoMessagesNotifications.notifyLocationServiceError(
                backgroundLocationError = backgroundLocationError,
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
                tag = "location_service",
                message = "Starting location service from $action"
            )
            LocationService.start(
                context = appClass,
                appForeground = isForeground,
                action = action
            )
            _pendingNotificationsChannel.trySend(null)
        } catch (e: Exception) {
            logg.w(
                tag = "location_service",
                message = "Can not start location service from $action",
                t = e
            )
            infoMessagesNotifications.notifyLocationServiceError(
                backgroundLocationError = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException,
                context = appClass
            )
        }
    }

    companion object {
        fun isPackageBootAction(action: String?): Boolean {
            return action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED
        }
    }
}