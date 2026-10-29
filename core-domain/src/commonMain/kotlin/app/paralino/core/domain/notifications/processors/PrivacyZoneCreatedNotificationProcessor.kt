package app.paralino.core.domain.notifications.processors

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.settings.NotificationGroupingType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PrivacyZoneCreatedNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val privacyZonesLocal: PrivacyZonesLocalDataSource,
    private val getPrivacyZonesUseCase: GetPrivacyZonesUseCase,
    private val appPrefs: AppPrefs,
    accessLogSource: AccessLogDataSource,
) : INotificationProcessor<PrivacyZoneCreatedNotificationProcessor.PrivacyZoneCreatedHookPayload>(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (PrivacyZoneCreatedHookPayload) -> Unit)?,
    ): IPushNotificationPresenter.Payload? {
        val payload = push.content?.tryParseFromJson<PrivacyZoneNotificationPayload>() ?: return null

        val groupId = payload.groupId.asGroupOrSpaceId()
        val zoneId = payload.privacyZoneId

        groupRefreshHook?.invoke(
            PrivacyZoneCreatedHookPayload(
                groupId = groupId,
                privacyZoneId = zoneId,
                cacheTimeDuration = 20.seconds,
            )
        )

        try {
            getPrivacyZonesUseCase.refreshPrivacyZoneMaybe(zoneId = zoneId)
        } catch (_: Exception) {
        }

        var zoneName: String? = null
        var groupName: String? = null
        var zoneOwner: String? = null

        try {
            val zone = privacyZonesLocal.getPrivacyZoneOnce(zoneId)
            zoneOwner = zone?.ownerUserId
            zoneName = zone?.nameOrNull
            groupName = groupRepo.getGroupOnce(groupId.id)?.nameOrNull
        } catch (_: Exception) {
        }

        val title = if (zoneName.isNullOrBlank() || groupName.isNullOrBlank()) {
            MR.strings.privacy_zone_created_notification_default.localizedString()
        } else {
            MR.strings.privacy_zone_created_notification.localizedString(zoneName, groupName)
        }

        val notificationGroup = when (appPrefs.notificationGrouping) {
            NotificationGroupingType.NONE -> null
            NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
            NotificationGroupingType.TYPE -> "privacy_zone_created"
            NotificationGroupingType.GROUP -> groupId.id
            NotificationGroupingType.PERSON -> zoneOwner ?: "privacy_zone_created"
        }

        return IPushNotificationPresenter.Payload(
            notificationId = "$groupId $zoneId".hashCode(),
            notificationGroupId = notificationGroup,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "privacy_zone_notification",
        )
    }

    data class PrivacyZoneCreatedHookPayload(
        override val groupId: GroupOrSpaceId,
        val privacyZoneId: String,
        override val cacheTimeDuration: Duration,
    ) : BaseNotificationProcessor.IGroupRefreshHookPayload

    @Serializable
    private data class PrivacyZoneNotificationPayload(
        val groupId: String,
        val privacyZoneId: String,
    )
}
