package app.paralino.core.repo.location

import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel

class LocationRepositoryWrapper(
    val locationRepo: LocationRepository
) {
    suspend fun pushLocation(data: LocationDataModel?): Payload<Unit> {
        return try {
            locationRepo.pushLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }
}