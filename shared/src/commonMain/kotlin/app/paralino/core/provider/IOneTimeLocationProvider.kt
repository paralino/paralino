package app.paralino.core.provider

import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

abstract class IOneTimeLocationProvider(
    protected val locationStatusProvider: ILocationStatusProvider,
    protected val locationCacheRepo: LocationCacheRepository
) {
    protected abstract suspend fun getLastLocationImpl(): LocationDataModel?

    suspend fun getLastLocation(): LocationDataModel? {
        val locationStatus = locationStatusProvider.currentStatus
        return if (locationStatus.isOK(forAlwaysOnAccess = locationStatus.isBackgroundPermissionAllowed)) {
            logg.v(tag = "location_service", message = "Try to fetch device location once")
            val lastDeviceLocation = try {
                withTimeoutOrNull(timeout = 4.seconds) {
                    try {
                        getLastLocationImpl()
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
            logg.v(tag = "location_service", message = "Got new location from one time fetch (${lastDeviceLocation != null})")
            val lastCacheLocation = locationCacheRepo.lastLocation
            LocationDataModel.getBetter(lastCacheLocation, lastDeviceLocation)
        } else {
            locationCacheRepo.lastLocation
        }
    }
}