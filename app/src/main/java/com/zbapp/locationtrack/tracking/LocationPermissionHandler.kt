package com.zbapp.locationtrack.tracking

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.shared.AppSnackbar
import com.zbapp.locationtrack.ui.shared.AppSnackbarData
import com.zbapp.locationtrack.utils.extensions.attributedBold

class LocationPermissionHandler(
    private val appClass: AppClass,
    private val activity: Activity,
    private val registry: ActivityResultRegistry
) : DefaultLifecycleObserver {


    constructor(fragment: Fragment) : this(
        fragment.requireActivity().application as AppClass,
        fragment.requireActivity(),
        fragment.requireActivity().activityResultRegistry
    ) {
        fragment.lifecycle.addObserver(this)
    }

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locationServiceLauncher: ActivityResultLauncher<Intent>

    private var snackBar: AppSnackbar? = null
    private var denied: Boolean = false


    override fun onCreate(owner: LifecycleOwner) {

        locationPermissionLauncher = registry.register(
            "key_location_${this.hashCode()}", owner, ActivityResultContracts.RequestMultiplePermissions()
        ) {
            denied = it.none { it.value }
            if (denied) {
                showLocationDeniedInfo()
            }
            updateStatus()
        }

        locationServiceLauncher = registry.register(
            "key_result_location_${this.hashCode()}", owner, ActivityResultContracts.StartActivityForResult()
        ) {
            askLocationService()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        denied = false
        updateStatus()
    }

    override fun onStop(owner: LifecycleOwner) {
        snackBar?.dismiss()
        snackBar = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        locationPermissionLauncher.unregister()
        locationServiceLauncher.unregister()
    }

    private fun updateStatus() {
        appClass.updateLocationStatus()
    }

    private fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun askLocationPermission() {
        when {
            LocationConditionStatus.hasLocationPermission(appClass) -> {
                updateStatus()
            }
            !shouldShowRationale() && denied -> {
                showLocationDeniedInfo()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    fun askLocationService() {
        if (LocationConditionStatus.isLocationOn(appClass)) {
            updateStatus()
        } else {
            showGPSOffInfo()
        }
    }

    private fun showLocationDeniedInfo() {
        snackBar = AppSnackbar.make(
            activity.window.peekDecorView()?.findViewById(R.id.mainActivityRoot),
            AppSnackbarData(
                text = appClass.getString(R.string.error_location_permission_missing_sheet_message).attributedBold(appClass),
                actionText = "Settings",
                duration = Snackbar.LENGTH_INDEFINITE
            )
        ) {
            snackBar?.dismiss()
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", appClass.packageName, null)
            }
            activity.startActivity(intent)
        }?.also { it.show() }
    }

    private fun showGPSOffInfo() {
        snackBar = AppSnackbar.make(
            activity.window.peekDecorView()?.findViewById(R.id.mainActivityRoot),
            AppSnackbarData(
                text = appClass.getString(R.string.error_location_service_missing_sheet_message).attributedBold(appClass),
                actionText = "Settings",
                duration = Snackbar.LENGTH_INDEFINITE
            )
        ) {
            snackBar?.dismiss()
            locationServiceLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }?.also { it.show() }
    }

}

