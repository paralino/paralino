package app.paralino.android.notification

import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.notifications.ILocationRequestNotificationsHandler
import app.paralino.core.notifications.LocationRequestNotificationPayload
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString

class LocationRequestNotificationHandler(
    private val locationPerformanceManager: LocationPerformanceManager
) : ILocationRequestNotificationsHandler {

    override fun process(push: PushNotificationRaw) {

        val payload = push.content?.let {
            BaseRemoteModule.formatter.decodeFromString<LocationRequestNotificationPayload>(it)
        } ?: return

        locationPerformanceManager.locationBoostRequested(
            groupId = payload.groupId,
            performanceType = LocationPerformanceType.VERY_HIGH
        )
    }

}