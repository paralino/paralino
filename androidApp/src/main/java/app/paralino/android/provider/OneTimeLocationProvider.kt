package app.paralino.android.provider

import app.paralino.android.tracking.LocationInitHandler
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class OneTimeLocationProvider(
    locationStatusProvider: ILocationStatusProvider,
    locationCacheRepo: LocationCacheRepository,
    private val locationInit: LocationInitHandler
) : IOneTimeLocationProvider(locationStatusProvider, locationCacheRepo) {
    override suspend fun fetchLastLocation(
        requestType: RequestType.Fetch
    ): LocationDataModel? {
        return locationInit.getOrFetchLastLocation(
            timeout = requestType.timeoutMillis.milliseconds,
            performanceType = requestType.type
        )
    }
}