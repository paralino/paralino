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

    private val requestStatusLiveData = MutableLiveData<LocationConditionStatus>()
    val requestStatus: LiveData<LocationConditionStatus> = requestStatusLiveData

    private val statusLiveData = MutableLiveData<LocationConditionStatus>()
    val status: LiveData<LocationConditionStatus> = statusLiveData


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun onResume(owner: LifecycleOwner) {
        updateStatus()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    override fun onCreate(owner: LifecycleOwner) {

        locationPermissionLauncher = registry.register(
            "key_location", owner, ActivityResultContracts.RequestPermission()
        ) {
            updateRequestStatus()
        }

        locationServiceLauncher = registry.register(
            "key_result_location", owner, ActivityResultContracts.StartActivityForResult()
        ) {
            updateRequestStatus()
        }
    }

    fun updateRequestStatus() {
        requestStatusLiveData.postValue(LocationConditionStatus.getLocationStatus(context))
        updateStatus()
    }

    fun updateStatus() {
        statusLiveData.postValue(LocationConditionStatus.getLocationStatus(context))
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
                updateRequestStatus()
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
            updateRequestStatus()
            return true
        }

        locationServiceLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        return false
    }

}

