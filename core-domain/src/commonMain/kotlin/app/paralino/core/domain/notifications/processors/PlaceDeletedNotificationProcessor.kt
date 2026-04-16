package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class PlaceDeletedNotificationProcessor(
    private val accessLogSource: AccessLogDataSource
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {
        val groupId = if (push.pushType == PushNotificationType.PERSONAL_PLACE_DELETED) {
            UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
        } else {
            push.content?.tryParseFromJson<PlaceDeleteNotificationPayload>()?.groupId?.asGroupOrSpaceId()
        }

        if (groupId == null) {
            return null
        }
        groupRefreshHook?.invoke(
            GroupRefreshHookPayload(
                groupId = groupId,
                cacheTimeDuration = 20.seconds
            )
        )
        return null
    }

    @Serializable
    private data class PlaceDeleteNotificationPayload constructor(
        val groupId: String,
        val placeId: String
    )
}