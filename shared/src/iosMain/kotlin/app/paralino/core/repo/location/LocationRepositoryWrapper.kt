package app.paralino.core.repo.location

import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel

class LocationRepositoryWrapper(
    val locationRepo: LocationUploadEngine
) {
    fun registerNewLocation(data: LocationDataModel): Payload<Unit> {
        return try {
            locationRepo.registerNewLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }
}