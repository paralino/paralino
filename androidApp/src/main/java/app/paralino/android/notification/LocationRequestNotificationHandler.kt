package app.paralino.android.notification

import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.notifications.ILocationRequestNotificationsHandler
import app.paralino.core.notifications.LocationRequestNotificationPayload
import app.paralino.core.repo.location.LocationPerformanceManager

class LocationRequestNotificationHandler(
    private val locationPerformanceManager: LocationPerformanceManager
) : ILocationRequestNotificationsHandler {

    override fun process(payload: LocationRequestNotificationPayload) {
        locationPerformanceManager.locationBoostRequested(
            groupId = payload.groupId,
            performanceType = LocationPerformanceType.VERY_HIGH
        )
    }

}