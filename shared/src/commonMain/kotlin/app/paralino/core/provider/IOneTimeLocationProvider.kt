package app.paralino.core.provider

import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

abstract class IOneTimeLocationProvider(
    protected val locationStatusProvider: ILocationStatusProvider,
    protected val locationCacheRepo: LocationCacheRepository
) {
    protected abstract suspend fun fetchLastLocation(
        requestType: RequestType.Fetch
    ): LocationDataModel?

    suspend fun getLastLocation(
        requestType: RequestType
    ): LocationDataModel? {
        val locationStatus = locationStatusProvider.currentStatus
        return if (locationStatus.isOK(forAlwaysOnAccess = locationStatus.isBackgroundPermissionAllowed)) {
            logg.v(tag = "location_service", message = "Try to fetch device location once")
            val lastDeviceLocation = when (requestType) {
                is RequestType.Fetch -> {
                    try {
                        withTimeoutOrNull(timeout = requestType.timeoutMillis.milliseconds.plus(2.seconds)) {
                            try {
                                fetchLastLocation(requestType = requestType)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                RequestType.CacheOnly -> {
                    null
                }
            }
            logg.v(tag = "location_service", message = "Got new location from one time fetch (${lastDeviceLocation != null})")
            val lastCacheLocation = locationCacheRepo.lastLocation
            LocationDataModel.getBetter(lastCacheLocation, lastDeviceLocation)
        } else {
            locationCacheRepo.lastLocation
        }
    }

    sealed interface RequestType {
        data object CacheOnly: RequestType
        data class Fetch(val timeoutMillis: Long, val type: LocationPerformanceType): RequestType
    }
}