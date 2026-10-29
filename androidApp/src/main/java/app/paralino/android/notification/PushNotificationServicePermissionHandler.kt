package app.paralino.android.notification

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.AppSnackbarResultType
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.provider.ConditionRequestFinishedEvent
import app.paralino.core.provider.ConditionRequestType
import app.paralino.core.provider.NotificationSocketServiceManager
import kotlinx.coroutines.launch

@Composable
fun rememberPushNotificationServicePermissionHandler(
    appState: MainAppState,
    messageDispatcher: ICoreMessageDispatcher,
    serviceManager: NotificationSocketServiceManager,
): PushNotificationServicePermissionHandlerState {

    val context = LocalContext.current

    return remember(context, appState, serviceManager) {
        PushNotificationServicePermissionHandlerState(
            appState = appState,
            messageDispatcher = messageDispatcher,
            context = context,
            serviceManager = serviceManager
        )
    }
}

class PushNotificationServicePermissionHandlerState constructor(
    val appState: MainAppState,
    val messageDispatcher: ICoreMessageDispatcher,
    val context: Context,
    val serviceManager: NotificationSocketServiceManager,
) {

    fun askPushNotificationService() {
        if (serviceManager.getServiceSetupError() == null) {
            requestFinished()
        } else {
            showServiceSetupInfo()
        }
    }

    private fun requestFinished() {
        messageDispatcher.onMessageEvent(ConditionRequestFinishedEvent(ConditionRequestType.PUSH_NOTIFICATION_SERVICE))
    }

    private fun showServiceSetupInfo() {
        appState.uiScope.launch {
            val result = messageDispatcher.showSnackbarForResult(
                AppSnackbarData(
                    text = MR.strings.error_push_notification_service_setup_message.getString(context),
                    actionText = MR.strings.button_open_settings.getString(context),
                    duration = SnackbarDuration.Long
                )
            )?.result

            if (result == AppSnackbarResultType.ActionPerformed) {
                appState.navController.navigate(AppScreen.Settings.NotificationSocket.routeSignature)
            } else {
                requestFinished()
            }
        }
    }
}
