package app.paralino.core.notifications

import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.notifications.processors.BaseNotificationProcessor
import app.paralino.core.domain.notifications.processors.PrivacyZoneCreatedNotificationProcessor
import app.paralino.core.logg
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesGetRemoteSource

class IOSNSEPrivacyZoneCreatedNotificationProcessor(
    private val privacyZoneCreatedNotification: PrivacyZoneCreatedNotificationProcessor,
    private val privacyZonesGetRemoteSource: PrivacyZonesGetRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    accessLogSource: AccessLogDataSource,
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?,
    ): IPushNotificationPresenter.Payload? {
        return privacyZoneCreatedNotification.constructPushPayload(
            push = push,
            groupRefreshHook = {
                try {
                    triggerRemoteOnly {
                        privacyZonesGetRemoteSource.getPrivacyZone(privacyZoneId = it.privacyZoneId)
                            .also { zone -> groupLocal.updateOrCreatePrivacyZone(zone) }
                    }
                } catch (e: Exception) {
                    logg.w("notifications", "Error refreshing privacy zone: ${e.message}")
                }
            },
        )
    }
}
