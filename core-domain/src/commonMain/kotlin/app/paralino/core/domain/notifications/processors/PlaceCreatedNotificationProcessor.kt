package app.paralino.core.domain.notifications.processors

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.models.settings.NotificationGroupingType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PlaceCreatedNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val placeRepo: PlacesLocalDataSource,
    private val appPrefs: AppPrefs,
    accessLogSource: AccessLogDataSource
): INotificationProcessor<PlaceCreatedNotificationProcessor.PlaceCreatedHookPayload>(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (PlaceCreatedHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {
        val (groupId, placeId) = try {
            push.content?.let {
                BaseRemoteModule.formatter.parseToJsonElement(it).jsonObject
            }?.let {
                val groupId = if (push.pushType == PushNotificationType.PERSONAL_PLACE_CREATED) {
                    UserGroup.groupPersonalDevicesId
                } else {
                    it.get("groupId")?.jsonPrimitive?.contentOrNull
                }
                val placeId = it.get("placeId")?.jsonPrimitive?.contentOrNull

                if (groupId == null || placeId == null) {
                    return null
                }

                groupId.asGroupOrSpaceId() to placeId
            } ?: return null
        } catch (e: Exception) {
            return null
        }

        groupRefreshHook?.invoke(
            PlaceCreatedHookPayload(
                groupId = groupId,
                placeId = placeId,
                cacheTimeDuration = 20.seconds
            )
        )

        var placeName: String? = null
        var groupName: String? = null
        var placeOwner: String? = null

        try {
            val place = placeRepo.getPlaceWithoutSettingsOnce(placeId)
            placeOwner = place?.ownerUserId
            placeName = place?.nameOrNull
            if (!groupId.isPersonalSpace) {
                groupName = groupRepo.getGroupOnce(groupId.id)?.nameOrNull
            }
        } catch (e: Exception) {
        }

        val title = if (groupId.isPersonalSpace) {
            if (placeName.isNullOrBlank()) {
                MR.strings.place_created_personal_notification_default.localizedString()
            } else {
                MR.strings.place_created_personal_notification.localizedString(placeName)
            }
        } else {
            if (placeName.isNullOrBlank() || groupName.isNullOrBlank()) {
                MR.strings.place_created_group_notification_default.localizedString()
            } else {
                MR.strings.place_created_group_notification.localizedString(groupName, placeName)
            }
        }

        val notificationGroup = when (appPrefs.notificationGrouping) {
            NotificationGroupingType.NONE -> null
            NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
            NotificationGroupingType.TYPE -> "place_created"
            NotificationGroupingType.GROUP -> groupId.id
            NotificationGroupingType.PERSON -> placeOwner ?: "place_created"
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = "$groupId $placeId".hashCode(),
            notificationGroupId = notificationGroup,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "place_notification"
        )

        return pushPayload
    }

    data class PlaceCreatedHookPayload constructor(
        override val groupId: GroupOrSpaceId,
        val placeId: String,
        override val cacheTimeDuration: Duration
    ): BaseNotificationProcessor.IGroupRefreshHookPayload
}