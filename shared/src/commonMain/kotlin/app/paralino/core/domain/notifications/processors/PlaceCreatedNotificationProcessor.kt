package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class PlaceCreatedNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val placeRepo: PlacesLocalDataSource,
    private val accessLogSource: AccessLogDataSource
): INotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<PlaceCreatedNotificationPayload>()
            ?: return null

        groupRefreshHook?.invoke(
            GroupRefreshHookPayload(
                groupId = payload.groupId,
                cacheTimeDuration = 20.seconds
            )
        )

        var placeName: String? = null
        var groupName: String? = null

        try {
            val place = placeRepo.getPlaceWithoutSettingsOnce(payload.placeId)
            placeName = place?.nameOrNull
            groupName = groupRepo.getGroupOnce(payload.groupId)?.nameOrNull
        } catch (e: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || groupName.isNullOrBlank()) {
            MR.strings.place_created_notification_default.localizedString()
        } else {
            MR.strings.place_created_notification.localizedString(groupName, placeName)
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = payload.hashCode(),
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "place_notification"
        )

        return pushPayload
    }

    @Serializable
    private data class PlaceCreatedNotificationPayload constructor(
        val groupId: String,
        val placeId: String
    )
}