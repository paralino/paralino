package com.zbapp.locationtrack.tracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*

class LocationPermissionHandler(
    private val context: Context,
    private val registry: ActivityResultRegistry
) : DefaultLifecycleObserver {


    constructor(fragment: Fragment) : this(
        fragment.requireContext(),
        fragment.requireActivity().activityResultRegistry
    ) {
        fragment.lifecycle.addObserver(this)
    }

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationServiceLauncher: ActivityResultLauncher<Intent>

    private var needPermissionRationale: Boolean = false

    private val statusLiveData = MutableLiveData<LocationConditionStatus>()
    val status: LiveData<LocationConditionStatus> = statusLiveData


//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    override fun onResume(owner: LifecycleOwner) {
//        updateStates()
//    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    override fun onCreate(owner: LifecycleOwner) {

        locationPermissionLauncher = registry.register(
            "key_location", owner, ActivityResultContracts.RequestPermission()
        ) {
            updateStatus()
        }

        locationServiceLauncher = registry.register(
            "key_result_location", owner, ActivityResultContracts.StartActivityForResult()
        ) {
            updateStatus()
        }
    }

    fun updateStatus() {
        if (!LocationConditionStatus.hasLocationPermission(context)) {
            statusLiveData.postValue(LocationConditionStatus.LOCATION_PERMISSION_DENIED)
        } else if (!LocationConditionStatus.isLocationOn(context)) {
            statusLiveData.postValue(LocationConditionStatus.LOCATION_SERVICE_DISABLED)
        } else {
            // all good
            statusLiveData.postValue(LocationConditionStatus.OK)
        }
    }


    fun askLocationPermission(activityForRationale: Activity? = null): Boolean {
        needPermissionRationale = if (activityForRationale == null)
            false
        else
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activityForRationale,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        when {
            LocationConditionStatus.hasLocationPermission(context) -> {
                updateStatus()
                return true
            }
            needPermissionRationale -> {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                return false
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return false
            }
        }
    }

    fun askLocationService(): Boolean {
        if (LocationConditionStatus.isLocationOn(context)) {
            updateStatus()
            return true
        }

        locationServiceLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return false
    }

}

