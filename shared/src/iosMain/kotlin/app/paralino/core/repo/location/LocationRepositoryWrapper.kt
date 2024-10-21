package app.paralino.core.repo.location

import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.notifications.PushNotificationHandler
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AccessLogDataSource

class LocationRepositoryWrapper(
    val locationRepo: LocationUploadEngine,
    private val locationSharingManager: LocationSharingManager,
    private val accessLogDataSource: AccessLogDataSource,
    private val pushNotificationHandler: PushNotificationHandler
) {

    fun registerNewLocation(data: LocationDataModel): Payload<Unit> {
        return try {
            locationRepo.registerNewLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }

    suspend fun startLocationRequest(): Boolean {
        val result = pushNotificationHandler.tryToHandleLocationPush()
        return result && canProcessLocationRequest()
    }

    suspend fun canProcessLocationRequest(): Boolean {
        if (!locationSharingManager.isSharingActive()) {
            return false
        }

        if (!accessLogDataSource.canProcessLocationRequest()) {
            return false
        }

        return true
    }

    suspend fun deleteLocationRequestLog() {
        accessLogDataSource.deleteLocationRequestAccessLog()
    }
}