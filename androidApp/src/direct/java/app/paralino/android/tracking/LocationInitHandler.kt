package app.paralino.android.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import app.paralino.android.provider.getLocationStatus
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationProviderType
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.LocationStatusException
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.extensions.stripNanoseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Instant

@SuppressLint("MissingPermission")
class LocationInitHandler constructor(
    private val context: Context,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val scope: ICoroutineScopeProvider,
    private val appPrefs: AppPrefs
) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationTrackingFlag = MutableStateFlow(false)
    val currentLocation = MutableStateFlow<Payload<LocationUpdateDataFromDevice>>(
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
                    logg.tag("location_request").v("Change location request: $performanceType")
                    val locationRequest = LocationRequestModels.getRequest(
                        type = performanceType.current,
                        balancedSettings = performanceType.balancedPerformanceSettings,
                        batterySaverSettings = performanceType.batterySaverPerformanceSettings
                    )

                    LocationManagerCompat.removeUpdates(locationManager, locationListener)
                    
                    logg.tag("location_request").v("Old request removed, start new request: $performanceType")
                    
                    val providers = locationManager.getProviders(true)
                    for (provider in providers) {
                        if (provider == LocationManager.PASSIVE_PROVIDER) continue
                        try {
                            LocationManagerCompat.requestLocationUpdates(
                                locationManager,
                                provider,
                                locationRequest,
                                locationListener,
                                Looper.getMainLooper()
                            )
                        } catch (e: Exception) {
                            logg.tag("location_request").e("Failed to request updates for $provider", e)
                        }
                    }
                } else {
                    logg.tag("location_request").v("status not ok: $status")
                    LocationManagerCompat.removeUpdates(locationManager, locationListener)
                    currentLocation.value = Payload.Error(LocationStatusException(status))
                }
            }
        }
            .launchIn(scope.defaultIOScope)
    }

    fun startLocationUpdates() {
        logg.tag("location_request").v("start location updates")
        locationTrackingFlag.value = true
    }

    fun stopLocationUpdates() {
        logg.tag("location_request").v("stopping all location updates")
        locationTrackingFlag.value = false
        currentLocation.value = Payload.Error(LocationStoppedException())
        LocationManagerCompat.removeUpdates(locationManager, locationListener)
    }

    suspend fun getOrFetchLastLocation(timeout: Duration, performanceType: LocationPerformanceType): LocationUpdateDataFromDevice? {
        return withTimeoutOrNull(timeout) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()

                val isGpsPreferred = when (performanceType) {
                    LocationPerformanceType.HIGH,
                    LocationPerformanceType.HIGHER_FOCUS,
                    LocationPerformanceType.VERY_HIGH,
                    LocationPerformanceType.MANUAL_BOOST,
                    LocationPerformanceType.MEDIUM_HIGH -> true
                    LocationPerformanceType.BALANCED -> appPrefs.locationSettingsBalancedHighAccuracy
                    LocationPerformanceType.BATTERY_SAVER,
                    LocationPerformanceType.HIGH_BATTERY_SAVER -> false
                }

                val provider = if (isGpsPreferred) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                    else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER
                    else LocationManager.PASSIVE_PROVIDER
                } else {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER
                    else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER
                    else LocationManager.PASSIVE_PROVIDER
                }

                if (provider == LocationManager.PASSIVE_PROVIDER) {
                    continuation.resume(locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.toLocationModel())
                    return@suspendCancellableCoroutine
                }

                try {
                    LocationManagerCompat.getCurrentLocation(
                        locationManager,
                        provider,
                        cancellationSignal,
                        ContextCompat.getMainExecutor(context)
                    ) { location ->
                        if (continuation.isActive) {
                            continuation.resume(location?.toLocationModel())
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                continuation.invokeOnCancellation { cancellationSignal.cancel() }
            }
        }
    }

    private var locationListener = LocationListenerCompat { location ->
        logg.tag("location_request").v("Got location update from system: ${location.provider}")
        currentLocation.value = Payload.Success(location.toLocationModel())
    }

    private fun Location.toLocationModel(): LocationUpdateDataFromDevice {
        return LocationUpdateDataFromDevice(
            lat = this.latitude,
            lng = this.longitude,
            rawHorizontalAccuracy = this.accuracy,
            timeUTC = Instant.fromEpochMilliseconds(this.time)
                .toLocalDateTime(TimeZone.UTC)
                .stripNanoseconds()
                .toString(),
            speed = if (this.hasSpeed()) this.speed else null,
            altitude = if (this.hasAltitude()) this.altitude.toFloat() else null,
            provider = LocationProviderType.fromFullKey(this.provider),
            isLikelyInaccurate = null
        )
    }
}

class LocationStoppedException() : Exception("Location tracking/sharing stopped")
