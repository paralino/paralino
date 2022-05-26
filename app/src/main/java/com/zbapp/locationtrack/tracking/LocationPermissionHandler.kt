package com.zbapp.locationtrack.tracking

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
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*
import com.google.android.material.snackbar.Snackbar
import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.onAction
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermissionHandler(
    appState: MainAppState,
    locationStatusProvider: ILocationStatusProvider,
): LocationPermissionHandlerState {

    val context = LocalContext.current

    val state = LocationPermissionHandlerState(
        appState = appState,
        context = context,
        locationStatusProvider = locationStatusProvider
    )

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
class LocationPermissionHandlerState constructor(
    val appState: MainAppState,
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
        locationStatusProvider.status.update { LocationConditionStatus.getLocationStatus(context) }
    }

    private fun showGPSOffInfo() {
        appState.uiScope.launch {
            appState.showSnackbar(
                AppSnackbarData(
                    text = context.getString(R.string.error_location_service_missing_sheet_message),
                    actionText = "Settings",
                    duration = Snackbar.LENGTH_LONG
                )
            ).onAction {
                gpsLauncher?.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun showLocationDeniedInfo() {

        val messageRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            R.string.error_location_precise_permission_missing_sheet_message
        } else R.string.error_location_permission_missing_sheet_message

        appState.uiScope.launch {
            appState.showSnackbar(
                AppSnackbarData(
                    text = context.getString(messageRes),
                    actionText = "Settings",
                    duration = Snackbar.LENGTH_LONG
                )
            ).onAction {
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
                R.string.error_location_background_permission_missing_sheet_message_android11,
                context.packageManager.backgroundPermissionOptionLabel
            )
        } else {
            context.getString(
                R.string.error_location_background_permission_missing_sheet_message,
            )
        }

        appState.uiScope.launch {
            appState.showSnackbar(
                AppSnackbarData(
                    text = message,
                    actionText = "Settings",
                    duration = Snackbar.LENGTH_LONG
                )
            ).onAction {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }

}