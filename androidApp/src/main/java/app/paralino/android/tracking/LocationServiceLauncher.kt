package app.paralino.android.tracking

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import app.paralino.android.notification.InfoMessagesNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LocationServiceLauncher(
    private val appClass: Application,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationSharingManager: LocationSharingManager,
    private val infoMessagesNotifications: InfoMessagesNotificationPresenter,
    private val scopeProvider: ICoroutineScopeProvider
) {
    private val _pendingNotificationsChannel = Channel<Boolean?>()

    init {
        scopeProvider.defaultIOScope.launch {
            try {
                _pendingNotificationsChannel
                    .receiveAsFlow()
                    .conflate()
                    .collectLatest { isBackgroundError ->
                        if (isBackgroundError != null) {
                            delay(2000)
                            infoMessagesNotifications.notifyLocationServiceError(
                                backgroundLocationError = isBackgroundError,
                                context = appClass
                            )
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
                if (locationSharingManager.isSharingActive()) {
                    val status = deviceApiProvider.locationStatusProvider.currentStatus
                    val hasPermissionForBackground = status.isOK(forAlwaysOnAccess = true)
                    val hasAnyPermission = status.areOnlyPermissionsOk(forAlwaysOnAccess = false)

                    if (!hasAnyPermission) {
                        logg.w(
                            tag = "location_service",
                            message = "Location sharing missing permissions for $action"
                        )
                        _pendingNotificationsChannel.trySend(true)
                    } else if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
                        // here we already have some location permission, but GPS might still be off
                        startLocationService(isForeground = true, action = action)
                    } else {
                        if (hasPermissionForBackground) {
                            startLocationService(isForeground = false, action = action)
                        } else {
                            logg.w(
                                tag = "location_service",
                                message = "App in background but missing background permissions for $action"
                            )
                            _pendingNotificationsChannel.trySend(true)
                        }
                    }
                }
            } else {
                logg.v("location_service", "Sharing not active, skip location service")
            }
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
            logg.e(
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
}