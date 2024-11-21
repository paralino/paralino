package app.paralino.core.notifications

import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.notifications.processors.BaseNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceCreatedNotificationProcessor
import app.paralino.core.logg
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesGetRemoteSource

class IOSNSEPlaceCreatedNotificationProcessor(
    private val placeCreatedNotification: PlaceCreatedNotificationProcessor,
    private val placesRemoteSource: PlacesGetRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    accessLogSource: AccessLogDataSource
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {
        return placeCreatedNotification.constructPushPayload(
            push = push,
            groupRefreshHook = {
                try {
                    triggerRemoteOnly {
                        placesRemoteSource.getPlaceWithSettings(placeId = it.placeId)
                            .also { groupLocal.updateOrCreatePlace(it) }
                    }
                } catch (e: Exception) {
                    logg.w("notifications", "Error refreshing place: ${e.message}")
                }
            }
        )
    }
}