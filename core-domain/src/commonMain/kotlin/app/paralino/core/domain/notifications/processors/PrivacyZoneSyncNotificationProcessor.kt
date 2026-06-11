package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.privacyzone.DeletePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class PrivacyZoneSyncNotificationProcessor(
    private val accessLogSource: AccessLogDataSource,
    private val getPrivacyZonesUseCase: GetPrivacyZonesUseCase,
    private val deletePrivacyZoneUseCase: DeletePrivacyZoneUseCase
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?,
    ): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<PrivacyZoneNotificationPayload>() ?: return null

        when (push.pushType) {
            PushNotificationType.GROUP_PRIVACY_ZONE_DELETED -> {
                deletePrivacyZoneUseCase.deletePrivacyZoneLocally(
                    groupId = payload.groupId,
                    privacyZoneId = payload.privacyZoneId,
                )
            }
            PushNotificationType.GROUP_PRIVACY_ZONE_UPDATED -> {
                getPrivacyZonesUseCase.refreshPrivacyZoneMaybe(zoneId = payload.privacyZoneId)
            }
            else -> Unit
        }

        groupRefreshHook?.invoke(
            GroupRefreshHookPayload(
                groupId = payload.groupId.asGroupOrSpaceId(),
                cacheTimeDuration = 20.seconds,
            )
        )
        return null
    }

    @Serializable
    private data class PrivacyZoneNotificationPayload(
        val groupId: String,
        val privacyZoneId: String,
    )
}
