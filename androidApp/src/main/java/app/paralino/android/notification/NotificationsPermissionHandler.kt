package app.paralino.android.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.provider.INotificationStatusProvider
import app.paralino.core.provider.NotificationPermissionResultEvent
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.onAction
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationsPermissionHandler(
    appState: MainAppState,
    messageDispatcher: ICoreMessageDispatcher,
    notificationStatusProvider: INotificationStatusProvider,
): NotificationsPermissionHandlerState {

    val context = LocalContext.current

    val state = remember(context, appState, notificationStatusProvider) {
        NotificationsPermissionHandlerState(
            appState = appState,
            messageDispatcher = messageDispatcher,
            context = context,
            notificationStatusProvider = notificationStatusProvider
        )
    }

    val notificationPermissionState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    ) {
        state.permissionRequestResult(it)
    }

    DisposableEffect(state, notificationPermissionState) {
        state.notificationPermissionState = notificationPermissionState
        onDispose { state.notificationPermissionState = null }
    }

    return state
}

@OptIn(ExperimentalPermissionsApi::class)
class NotificationsPermissionHandlerState constructor(
    val appState: MainAppState,
    val messageDispatcher: ICoreMessageDispatcher,
    val context: Context,
    val notificationStatusProvider: INotificationStatusProvider,
) {

    internal var notificationPermissionState: MultiplePermissionsState? = null
    private var denied: Boolean = false

    fun permissionRequestResult(state: Map<String, Boolean>) {
        denied = state.any { !it.value }

        val handled = updateStatus(granted = !denied)
        if (denied && !handled) {
            showPermissionDeniedInfo()
        }
    }

    fun askNotificationPermission() {
        when {
            notificationPermissionState?.allPermissionsGranted == true -> {
                updateStatus(granted = true)
            }
            notificationPermissionState?.shouldShowRationale == true -> {
                showPermissionDeniedInfo()
            }
            else -> {
                notificationPermissionState?.launchMultiplePermissionRequest()
            }
        }
    }

    private fun updateStatus(granted: Boolean): Boolean {
        notificationStatusProvider.refreshNotificationsStatus()
        return messageDispatcher.onMessageEvent(NotificationPermissionResultEvent(granted = granted))
    }

    private fun showPermissionDeniedInfo() {

        val messageRes = MR.strings.error_notification_permission_missing_sheet_message.resourceId

        appState.uiScope.launch {
            messageDispatcher.showSnackbarForResult(
                AppSnackbarData(
                    text = context.getString(messageRes),
                    actionText = MR.strings.button_open_settings.getString(context),
                    duration = SnackbarDuration.Long
                )
            )?.result?.onAction {
                openSettings()
            }
        }
    }

    private fun openSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}