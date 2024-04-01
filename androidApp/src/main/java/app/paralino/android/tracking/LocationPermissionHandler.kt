package app.paralino.android.tracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.ICoreMessageDispatcher
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.onAction
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermissionHandler(
    appState: MainAppState,
    messageDispatcher: ICoreMessageDispatcher,
    locationStatusProvider: ILocationStatusProvider,
): LocationPermissionHandlerState {

    val context = LocalContext.current

    val state = remember(context, appState, locationStatusProvider) {
        LocationPermissionHandlerState(
            appState = appState,
            messageDispatcher = messageDispatcher,
            context = context,
            locationStatusProvider = locationStatusProvider
        )
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        state.askLocationService()
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    ) {
        state.locationRequestResult(it)
    }

    val backgroundLocationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) {
            state.backgroundLocationRequestResult(it)
        }
    } else {
        null
    }

    DisposableEffect(state, gpsLauncher) {
        state.gpsLauncher = gpsLauncher
        onDispose { state.gpsLauncher = null }
    }

    DisposableEffect(state, locationPermissionsState) {
        state.locationPermissionsState = locationPermissionsState
        onDispose { state.locationPermissionsState = null }
    }

    DisposableEffect(state, backgroundLocationPermissionState) {
        state.backgroundLocationPermissionState = backgroundLocationPermissionState
        onDispose { state.backgroundLocationPermissionState = null }
    }

    return state
}

@OptIn(ExperimentalPermissionsApi::class)
data class LocationPermissionHandlerState constructor(
    val appState: MainAppState,
    val messageDispatcher: ICoreMessageDispatcher,
    val context: Context,
    val locationStatusProvider: ILocationStatusProvider,
) {
    internal var locationPermissionsState: MultiplePermissionsState? = null
    internal var backgroundLocationPermissionState: PermissionState? = null
    internal var gpsLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    private var denied: Boolean = false
    private var backgroundDenied: Boolean = false


    fun locationRequestResult(state: Map<String, Boolean>) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            backgroundDenied = state.filter {
                it.key == Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }.none { it.value }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            denied = state.filter {
                it.key != Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }.any { !it.value }
        } else {
            denied = state.any { !it.value }
        }

        if (denied) {
            showLocationDeniedInfo()
        } else if (backgroundDenied) {
            showBackgroundLocationDeniedInfo()
        }
        updateStatus()
    }

    fun backgroundLocationRequestResult(state: Boolean) {
        backgroundDenied = !state
        if (!state) {
            showBackgroundLocationDeniedInfo()
        }
        updateStatus()
    }

    fun askLocationPermission() {
        when {
            locationPermissionsState?.allPermissionsGranted == true -> {
                updateStatus()
            }

            locationPermissionsState?.shouldShowRationale?.not() == true && backgroundDenied -> {
                showLocationDeniedInfo()
            }

            else -> {
                locationPermissionsState?.launchMultiplePermissionRequest()
            }
        }
    }

    fun askBackgroundLocationPermission() {
        when {
            backgroundLocationPermissionState?.status?.isGranted == true -> {
                updateStatus()
            }

            backgroundLocationPermissionState?.status?.shouldShowRationale?.not() == true && backgroundDenied -> {
                showBackgroundLocationDeniedInfo()
            }

            else -> {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    locationPermissionsState?.launchMultiplePermissionRequest()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    backgroundLocationPermissionState?.launchPermissionRequest()
                }
            }
        }
    }

    fun askLocationService() {
        if (LocationConditionStatus.isLocationOn(context)) {
            updateStatus()
        } else {
            showGPSOffInfo()
        }
    }

    private fun updateStatus() {
        locationStatusProvider.refreshLocationStatus()
    }

    private fun showGPSOffInfo() {
        appState.uiScope.launch {
            messageDispatcher.showSnackbarForResult(
                AppSnackbarData(
                    text = context.getString(MR.strings.error_location_service_missing_sheet_message.resourceId),
                    actionText = "Settings",
                    duration = SnackbarDuration.Long
                )
            )?.result?.onAction {
                gpsLauncher?.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun showLocationDeniedInfo() {

        val messageRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MR.strings.error_location_precise_permission_missing_sheet_message
        } else MR.strings.error_location_permission_missing_sheet_message

        appState.uiScope.launch {
            messageDispatcher.showSnackbarForResult(
                AppSnackbarData(
                    text = context.getString(messageRes.resourceId),
                    actionText = "Settings",
                    duration = SnackbarDuration.Long
                )
            )?.result?.onAction {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

    private fun showBackgroundLocationDeniedInfo() {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getString(
                MR.strings.error_location_background_permission_missing_sheet_message_android11.resourceId,
                context.packageManager.backgroundPermissionOptionLabel
            )
        } else {
            context.getString(
                MR.strings.error_location_background_permission_missing_sheet_message.resourceId,
            )
        }

        appState.uiScope.launch {
            messageDispatcher.showSnackbarForResult(
                AppSnackbarData(
                    text = message,
                    actionText = "Settings",
                    duration = SnackbarDuration.Long
                )
            )?.result?.onAction {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

}