package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class PlaceUpdatedNotificationProcessor(
    private val accessLogSource: AccessLogDataSource
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<PlaceUpdatedNotificationPayload>()
            ?: return null
        groupRefreshHook?.invoke(
            GroupRefreshHookPayload(
                groupId = payload.groupId,
                cacheTimeDuration = 20.seconds
            )
        )
        return null
    }

    @Serializable
    private data class PlaceUpdatedNotificationPayload constructor(
        val groupId: String,
        val memberUserId: String
    )
}