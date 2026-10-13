package app.paralino.android.provider

import app.paralino.android.tracking.LocationInitHandler
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import kotlin.time.Duration.Companion.milliseconds

class OneTimeLocationProvider(
    locationStatusProvider: ILocationStatusProvider,
    locationCacheRepo: LocationCacheRepository,
    private val locationInit: LocationInitHandler
) : IOneTimeLocationProvider(locationStatusProvider, locationCacheRepo) {
    override suspend fun fetchLastLocation(
        requestType: RequestType.Fetch
    ): LocationUpdateDataFromDevice? {
        return locationInit.getOrFetchLastLocation(
            timeout = requestType.timeoutMillis.milliseconds,
            performanceType = requestType.type
        )
    }
}