package app.paralino.android.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import app.paralino.android.utils.extensions.millisToDateTimeUTC
import app.paralino.android.utils.extensions.toLocalString
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.LocationStatusException
import app.paralino.core.repo.location.LocationPerformanceManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SuppressLint("MissingPermission")
class LocationInitHandler constructor(
    private val context: Context,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val scope: ICoroutineScopeProvider,
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationTrackingFlag = MutableStateFlow(false)
    val currentLocation = MutableStateFlow<Payload<LocationDataModel>>(
        Payload.Error(LocationStoppedException())
    )

    val isTracking: Boolean get() = currentLocation.value is Payload.Success

    init {
        combine(
            locationTrackingFlag,
            locationPerformanceManager.locationPerformanceType
        ) { tracking, performanceType ->
            logg.tag("location_request").v("location request: ${performanceType.current}")
            if (tracking) {
                logg.tag("location_request").v("location request OK: ${performanceType.current.name}")
                val status = LocationConditionStatus.getLocationStatus(context)
                if (status.isOK()) {
                    logg.tag("location_request").v("new request: $performanceType")
                    val locationRequest = LocationRequestModels.getRequest(performanceType.current)
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    ).addOnFailureListener {
                        logg.tag("location_request").v("request error", it)
                        currentLocation.value = Payload.Error(it)
                    }
                } else {
                    logg.tag("location_request").v("status not ok: $status")
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    currentLocation.value = Payload.Error(LocationStatusException(status))
                }
            }
        }
            .launchIn(scope.defaultIOScope)
    }

//    val locationFlow = fusedLocationClient.locationFlow()
//        .stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), Payload.Loading)
//
//    fun FusedLocationProviderClient.locationFlow(): Flow<Payload<LocationModel>>{
//        return callbackFlow {
//
//            if(!LocationConditionStatus.isLocationStatusOk(context)) {
//                val error = Throwable("Location status != OK")
//                this.offer(Payload.Error(error))
//                close(error)
//                return@callbackFlow
//            }
//
//            val callback = object : LocationCallback() {
//                override fun onLocationResult(result: LocationResult?) {
//                    result ?: return
//                    for (location in result.locations) {
//                        try {
//                            offer(Payload.Success(location))
//                        } catch (e: Exception) {
//                        }
//                    }
//                }
//            }
//
//            requestLocationUpdates(
//                locationRequest,
//                callback,
//                Looper.getMainLooper()
//            ).addOnFailureListener { close(it) }
//
//            awaitClose {
//                this.offer(Payload.Error(Throwable("canceled")))
//                removeLocationUpdates(callback)
//            }
//
//        }
//    }

    fun startLocationUpdates() {
        logg.tag("location_request").v("start location updates")
        locationTrackingFlag.value = true
    }

    fun stopLocationUpdates() {
        logg.tag("location_request").v("stopping all location updates")
        locationTrackingFlag.value = false
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    suspend fun getOrFetchLastLocation(timeout: Duration, performanceType: LocationPerformanceType): LocationDataModel? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            val priority = LocationRequestModels.getRequest(performanceType).priority
            val request = CurrentLocationRequest.Builder()
                .setDurationMillis(timeout.inWholeMilliseconds)
                .setMaxUpdateAgeMillis(20.seconds.inWholeMilliseconds)
                .setPriority(priority)
                .build()

            fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener {
                    continuation.resume(it?.toLocationModel())
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }

            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(location: LocationResult) {
            logg.tag("location_request").v("Got location update from system")
            location.lastLocation?.let {
                currentLocation.value = Payload.Success(it.toLocationModel())
            }
        }
    }

    private fun Location.toLocationModel(): LocationDataModel {
        return LocationDataModel(
            lat = this.latitude,
            lng = this.longitude,
            accuracy = this.accuracy,
            timeUTC = this.time.millisToDateTimeUTC().toLocalString(),
            speed = if (this.hasSpeed()) this.speed else null,
            altitude = if (this.hasAltitude()) this.altitude.toFloat() else null
        )
    }
}

class LocationStoppedException() : Exception("Location tracking/sharing stopped")