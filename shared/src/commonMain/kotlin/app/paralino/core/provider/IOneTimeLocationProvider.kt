package app.paralino.core.provider

import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.getIfBetter
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
            val lastCacheLocation = locationCacheRepo.lastLocation
            lastCacheLocation.getIfBetter(lastDeviceLocation)
        } else {
            locationCacheRepo.lastLocation
        }
    }
}