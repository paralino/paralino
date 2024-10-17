package app.paralino.core.repo.location

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AccessLogDataSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class LocationRepositoryWrapper(
    val locationRepo: LocationUploadEngine,
    private val locationSharingManager: LocationSharingManager,
    private val appBuildConfig: AppBuildConfig,
    private val accessLogDataSource: AccessLogDataSource
) : LimitedExecutionRepository() {

    private val executionKey = "location_request"

    fun registerNewLocation(data: LocationDataModel): Payload<Unit> {
        return try {
            locationRepo.registerNewLocation(data)
            Payload.Success(Unit)
        } catch (e: Exception) {
            Payload.Error(e)
        }
    }

    suspend fun startLocationRequest(): Boolean {
        try {
            startProcessing(
                key = executionKey,
                delayedThresholdMillis = 5.seconds.inWholeMilliseconds
            )
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't complete location request")
            return false
        }

        try {
            accessLogDataSource.saveLocationRequestAccessLog()
            return true
        } catch (e: Exception) {
            logg.tag(executionKey).v("Failed to start location request: ${e.message}")
            return false
        } finally {
            stopProcessing(executionKey)
        }
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