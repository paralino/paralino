package app.paralino.android.provider

import app.paralino.android.tracking.LocationInitHandler
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.IOneTimeLocationProvider

class OneTimeLocationProvider(
    locationStatusProvider: ILocationStatusProvider,
    locationCacheRepo: LocationCacheRepository,
    private val locationInit: LocationInitHandler
) : IOneTimeLocationProvider(locationStatusProvider, locationCacheRepo) {
    override suspend fun getLastLocationImpl(): LocationDataModel? {
        return locationInit.getOrFetchLastLocation()
    }
}