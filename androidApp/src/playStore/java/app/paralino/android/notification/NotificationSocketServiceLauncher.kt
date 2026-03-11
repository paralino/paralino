package app.paralino.android.notification

import android.app.Application
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationSocketServiceManager
import kotlinx.coroutines.CoroutineScope

class NotificationSocketServiceLauncher(
    private val appClass: Application,
    private val deviceApiProvider: IDeviceApiProvider,
    private val infoMessagesNotifications: InfoMessagesNotificationPresenter,
    private val serviceManager: NotificationSocketServiceManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val updateUserData: UpdateUserDataUseCase
) {

    fun startMonitoring(scope: CoroutineScope) {}

    fun startNotificationService(
        action: String?
    ) {
        logg.w(
            tag = "notification_socket_service",
            message = "Notification socket service not installed"
        )
    }

    companion object {
        const val INTENT_NOTIFICATION_SOCKET_SERVICE_START = "NO-OP"
    }
}