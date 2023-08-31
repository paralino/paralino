package app.paralino.android.notification

import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.notifications.ILocationRequestNotificationsHandler
import app.paralino.core.notifications.LocationRequestNotificationPayload
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.seconds

class LocationRequestNotificationHandler(
    private val locationPerformanceManager: LocationPerformanceManager,
    private val sharingManager: LocationSharingManager,
    private val locationRepository: LocationRepository,
    private val coroutineScopeProvider: ICoroutineScopeProvider
) : LimitedExecution(), ILocationRequestNotificationsHandler {

    override fun process(push: PushNotificationRaw) {

        val payload = push.content?.let {
            BaseRemoteModule.formatter.decodeFromString<LocationRequestNotificationPayload>(it)
        }

        if (payload == null) {
            logg.w("Location request payload is NULL")
            return
        }

        coroutineScopeProvider.userSessionIOScope.launch {
            try {
                startProcessing(
                    key = "location_request",
                    delayedThresholdMillis = 5.seconds.inWholeMilliseconds
                )
                updateStatus(payload)
            } catch (e: Exception) {
                logg.w("Location request already in progress", e)
            }
        }
    }

    private suspend fun updateStatus(payload: LocationRequestNotificationPayload) {
        if (!sharingManager.isSharingActiveForGroup(id = payload.groupId)) {
            try {
                locationRepository.pushDeviceStatus(payload.groupId)
            } catch (e: Exception) {
                logg.w("Error saving location status", e)
            }
            return
        }

        locationPerformanceManager.locationBoostRequested(
            groupId = payload.groupId,
            performanceType = LocationPerformanceType.VERY_HIGH
        )

        try {
            locationRepository.pushDeviceStatus(payload.groupId)
        } catch (e: Exception) {
            logg.w("Error saving location status", e)
        }
    }
}