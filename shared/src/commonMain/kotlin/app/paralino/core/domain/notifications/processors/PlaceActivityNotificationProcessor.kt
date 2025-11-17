package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.presentation.settings.generic.NotificationGroupingType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

class PlaceActivityNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val placeRepo: PlacesLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val cryptoCore: CryptoCore,
    private val accessLogSource: AccessLogDataSource,
    private val personalDevicesLocal: PersonalDevicesLocalDataSource,
    private val appPrefs: AppPrefs
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<PlaceActivityNotificationPayload>()
            ?: return null

        var placeName: String? = null
        var objectName: String? = null
        var eventSeed: String? = null
        var groupId: GroupOrSpaceId? = null

        try {
            val place = placeRepo.getPlaceWithoutSettingsOnce(payload.placeId)
            groupId = place?.groupOrSpaceId
            placeName = place?.data?.data?.name
            if (groupId?.isPersonalSpace == true) {
                val deviceName = if (payload.locationId.isNullOrBlank()) {
                    null
                } else {
                    val device = personalDevicesLocal.getUserDeviceByIdOnce(payload.locationId)
                    device?.deviceNameOrNull
                }
                if (deviceName.isNullOrBlank()) {
                    objectName = MR.strings.places_notification_personalDevice_placeholder.localizedString()
                } else {
                    objectName = MR.strings.places_notification_personalDeviceName_placeholder.localizedString(deviceName)
                }
            } else {
                objectName = groupRepo.getMemberNameOnce(
                    groupId = groupId!!.id,
                    userId = payload.memberUserId
                )
            }
            eventSeed = place.eventSeed
        } catch (_: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || objectName.isNullOrBlank()) {
            MR.strings.place_notification_default.localizedString()
        } else {
            val isEnter = when {
                payload.event != null && eventSeed != null -> {
                    when (payload.event) {
                        cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true) -> true
                        cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false) -> false
                        else -> null
                    }
                }

                payload.enter != null -> payload.enter
                else -> null
            }

            if (groupId?.isPersonalSpace == true) {
                when (isEnter) {
                    true -> MR.strings.place_notification_device_enter.localizedString(objectName, placeName)
                    false -> MR.strings.place_notification_device_exit.localizedString(objectName, placeName)
                    else -> MR.strings.place_notification_personal_default.localizedString()
                }
            } else {
                when (isEnter) {
                    true -> MR.strings.place_notification_enter.localizedString(objectName, placeName)
                    false -> MR.strings.place_notification_exit.localizedString(objectName, placeName)
                    else -> MR.strings.place_notification_default.localizedString()
                }
            }
        }

        val triggerTime = payload.triggerAt?.isoWithOffsetToInstant() ?: push.createdAt
        val notificationId = "place_activity_${payload}_${triggerTime}".hashCode()
        val timeInfo = if (triggerTime.isOlderThan(1.minutes)) {
            valueFormatter.formatSimpleDateTime(
                timeUTC = triggerTime.toLocalDateTime(TimeZone.UTC),
                optionalYear = true,
                optionalMonth = false,
                showTodayInfo = false
            )
        } else {
            null
        }

        val notificationGroup = when (appPrefs.notificationGrouping) {
            NotificationGroupingType.NONE -> null
            NotificationGroupingType.EVERYTHING -> IPushNotificationPresenter.notificationGroupSystem
            NotificationGroupingType.TYPE -> "place_activity"
            NotificationGroupingType.GROUP -> groupId?.id ?: payload.placeId
            NotificationGroupingType.PERSON -> payload.memberUserId
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = notificationId,
            notificationGroupId = notificationGroup,
            title = title,
            message = null,
            timeMillis = triggerTime.toEpochMilliseconds(),
            timeInfo = timeInfo,
            loggerTag = "place_notification"
        )

        return pushPayload
    }

    @Serializable
    private data class PlaceActivityNotificationPayload constructor(
        val memberUserId: String,
        val locationId: String?,
        val placeId: String,

        @Deprecated("Use event instead")
        val enter: Boolean? = null,

        val event: String? = null,
        val triggerAt: String? = null
    )
}