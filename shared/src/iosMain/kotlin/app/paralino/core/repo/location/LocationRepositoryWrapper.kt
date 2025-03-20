package app.paralino.core.repo.location

import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.provider.CoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import kotlinx.coroutines.launch

class LocationRepositoryWrapper(
    private val locationRepo: LocationUploadEngine,
    private val locationSharingManager: LocationSharingManager,
    private val scopeProvider: CoroutineScopeProvider
) {

    fun startLocationUploadEngine() {
        locationRepo.startLocationUploadEngine()

        scopeProvider.userSessionIOScope.launch {
            locationRepo.updateDeviceStatusOnly(tryToFetchCachedLocation = false)
        }
    }

    suspend fun canProcessLocationRequest(): Boolean {
        return locationSharingManager.isSharingActive()
    }

    fun registerNewLocation(data: LocationDataModel): Payload<Unit> {
        return try {
            locationRepo.registerNewLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }
}