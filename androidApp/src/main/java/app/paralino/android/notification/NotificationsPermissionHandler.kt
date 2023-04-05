package app.paralino.android.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.AppSnackbarData
import app.paralino.android.ui.components.SnackbarDuration
import app.paralino.android.ui.components.onAction
import app.paralino.core.provider.ILocationStatusProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationsPermissionHandler(
    appState: MainAppState,
    locationStatusProvider: ILocationStatusProvider,
): NotificationsPermissionHandlerState {

    val context = LocalContext.current

    val state = NotificationsPermissionHandlerState(
        appState = appState,
        context = context,
        locationStatusProvider = locationStatusProvider
    )

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
    val context: Context,
    val locationStatusProvider: ILocationStatusProvider,
) {

    internal var notificationPermissionState: MultiplePermissionsState? = null
    private var denied: Boolean = false

    fun permissionRequestResult(state: Map<String, Boolean>) {
        denied = state.any { !it.value }

        if (denied) {
            showPermissionDeniedInfo()
        }
        updateStatus()
    }

    fun askNotificationPermission() {
        when {
            notificationPermissionState?.allPermissionsGranted == true -> {
                updateStatus()
            }
            notificationPermissionState?.shouldShowRationale == true -> {
                openSettings()
            }
            else -> {
                notificationPermissionState?.launchMultiplePermissionRequest()
            }
        }
    }

    private fun updateStatus() {
        locationStatusProvider.refreshLocationStatus()
    }

    private fun showPermissionDeniedInfo() {

        val messageRes = R.string.error_notification_permission_missing_sheet_message

        appState.uiScope.launch {
            appState.showSnackbar(
                AppSnackbarData(
                    text = context.getString(messageRes),
                    actionText = "Settings",
                    duration = SnackbarDuration.Long
                )
            ).onAction {
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